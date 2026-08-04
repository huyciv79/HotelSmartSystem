import { useState, useCallback, useRef } from 'react';
import {
  ShieldCheck, IdCard, Camera, CheckCircle2, XCircle,
  ChevronRight, RotateCcw, AlertCircle, Info
} from 'lucide-react';
import EkycCamera from './EkycCamera';
import EkycLivenessCamera from './EkycLivenessCamera';

// ─── Step indicator bar ───────────────────────────────────────────────────────
const STEPS = [
  { id: 1, label: 'Giới thiệu' },
  { id: 2, label: 'Mặt trước' },
  { id: 3, label: 'Mặt sau' },
  { id: 4, label: 'Khuôn mặt' },
  { id: 5, label: 'Xác minh' },
  { id: 6, label: 'Kết quả' },
];

function StepBar({ current }) {
  return (
    <div className="flex items-center justify-center gap-1 mb-8 select-none">
      {STEPS.map((s, idx) => {
        const done = s.id < current;
        const active = s.id === current;
        return (
          <div key={s.id} className="flex items-center gap-1">
            <div className={`
              flex items-center justify-center w-7 h-7 rounded-none text-xs font-semibold font-['Montserrat'] transition-all duration-300
              ${done ? 'bg-emerald-600 text-white' : active ? 'bg-primary text-white font-bold' : 'bg-slate-100 text-slate-400'}
            `}>
              {done ? <CheckCircle2 size={13} /> : s.id}
            </div>
            {idx < STEPS.length - 1 && (
              <div className={`w-5 h-[1.5px] transition-all duration-500 ${done ? 'bg-emerald-600' : 'bg-slate-200'}`} />
            )}
          </div>
        );
      })}
    </div>
  );
}

// ─── Image preview card ───────────────────────────────────────────────────────
function ImagePreviewCard({ label, previewUrl }) {
  if (!previewUrl) return null;
  return (
    <div className="flex flex-col items-center gap-1.5 font-['Montserrat']">
      <span className="text-[10px] text-slate-400 uppercase tracking-wider font-semibold">{label}</span>
      <img
        src={previewUrl}
        alt={label}
        className="w-24 h-16 object-cover rounded-none border border-slate-200 shadow-sm"
      />
    </div>
  );
}

// ─── Main RegisterEkyc wizard ─────────────────────────────────────────────────
/**
 * @param {(front: File, back: File, faceFrames: object) => Promise<void>} onSubmit
 * @param {() => void} onClose
 * @param {'register' | 'update'} mode
 */
export default function RegisterEkycWizard({ onSubmit, onClose, mode = 'register' }) {
  const [step, setStep] = useState(1);
  const [images, setImages] = useState({ front: null, back: null, faceFrames: null });
  const [previews, setPreviews] = useState({ front: null, back: null, faceFrames: null });
  const [result, setResult] = useState(null); // { success, message }
  const imagesRef = useRef({ front: null, back: null, faceFrames: null });

  const handleCapture = useCallback((key, nextStep) => (file, previewUrl) => {
    imagesRef.current[key] = file;
    setImages(prev => ({ ...prev, [key]: file }));
    setPreviews(prev => ({ ...prev, [key]: previewUrl }));
    setStep(nextStep);
  }, []);

  const handleLivenessComplete = useCallback(async (faceFrames, facePreviews) => {
    imagesRef.current.faceFrames = faceFrames;
    setImages(prev => ({ ...prev, faceFrames }));
    setPreviews(prev => ({ ...prev, faceFrames: facePreviews }));
    setStep(5);

    try {
      const front = imagesRef.current.front || images.front;
      const back = imagesRef.current.back || images.back;
      if (!front || !back) {
        throw new Error('Thiếu hình ảnh CCCD. Vui lòng chụp lại mặt trước và mặt sau.');
      }
      const res = await onSubmit(front, back, faceFrames);
      const successMessage = res?.data?.message || res?.message || 'Xác minh danh tính thành công! Tài khoản của bạn đã được kích hoạt đầy đủ.';
      setResult({ success: true, message: successMessage });
    } catch (err) {
      const msg = err?.response?.data?.message
        || err?.response?.data?.detail
        || err?.message
        || 'Xác minh thất bại. Vui lòng thử lại.';
      setResult({ success: false, message: msg });
    } finally {
      setStep(6);
    }
  }, [images.front, images.back, onSubmit]);

  const handleRetry = () => {
    imagesRef.current = { front: null, back: null, faceFrames: null };
    setStep(1);
    setImages({ front: null, back: null, faceFrames: null });
    setPreviews({ front: null, back: null, faceFrames: null });
    setResult(null);
  };

  const errorDetails = result?.success === false
    ? String(result.message || '')
      .split(';')
      .map((message) => message.trim())
      .filter(Boolean)
    : [];

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4"
      style={{ background: 'rgba(0,0,0,0.65)', backdropFilter: 'blur(4px)' }}>
      <div className="
        relative w-full max-w-lg bg-white
        rounded-none shadow-2xl
        border border-outline-variant overflow-hidden
        flex flex-col text-slate-800 font-['Montserrat']
      ">
        {/* Close button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 z-10 w-8 h-8 rounded-none border border-slate-200 flex items-center justify-center text-slate-400 hover:text-slate-950 hover:bg-slate-50 transition-all cursor-pointer bg-transparent"
        >
          ✕
        </button>

        {/* Scrollable body */}
        <div className="overflow-y-auto max-h-[90vh] p-6 sm:p-8 flex flex-col gap-6">
          <StepBar current={step} />

          {/* ── Step 1: Introduction ── */}
          {step === 1 && (
            <div className="flex flex-col items-center gap-6 text-center animate-fade-in">
              <div className="w-16 h-16 rounded-none bg-primary/10 border border-primary/20 flex items-center justify-center text-primary shadow-sm animate-fade-in">
                <ShieldCheck size={30} />
              </div>
              <div>
                <h2 className="text-xl font-semibold text-slate-900 uppercase tracking-wider mb-2">
                  {mode === 'update' ? 'Cập nhật eKYC' : 'Xác minh danh tính'}
                </h2>
                <p className="text-slate-500 text-xs font-semibold uppercase tracking-wider leading-relaxed">
                  Quy trình xác minh danh tính điện tử giúp bảo vệ tài khoản của bạn.
                </p>
              </div>

              <div className="w-full bg-slate-50 border border-slate-200 rounded-none p-5 text-left flex flex-col gap-4">
                <p className="text-primary text-xs font-semibold uppercase tracking-widest flex items-center gap-2 border-b border-slate-200/50 pb-2">
                  <Info size={14} /> Hướng dẫn chuẩn bị:
                </p>
                <div className="flex flex-col gap-3">
                  <div className="flex items-start gap-2.5 text-xs text-slate-700 font-semibold uppercase tracking-wide">
                    <IdCard size={14} className="text-primary shrink-0 mt-0.5" />
                    <span>CCCD/CMND còn hiệu lực và rõ ràng</span>
                  </div>
                  <div className="flex items-start gap-2.5 text-xs text-slate-700 font-semibold uppercase tracking-wide">
                    <Info size={14} className="text-primary shrink-0 mt-0.5" />
                    <span>Chọn nơi đủ sáng, tránh ngược sáng</span>
                  </div>
                  <div className="flex items-start gap-2.5 text-xs text-slate-700 font-semibold uppercase tracking-wide">
                    <CheckCircle2 size={14} className="text-primary shrink-0 mt-0.5" />
                    <span>Đặt thẻ phẳng, không che khuất thông tin</span>
                  </div>
                </div>
              </div>

              <button
                onClick={() => setStep(2)}
                className="w-full py-3.5 bg-primary hover:bg-slate-950 text-white font-semibold uppercase text-xs tracking-widest transition-all duration-300 cursor-pointer border-none flex items-center justify-center gap-1.5 rounded-none parallelogram-btn"
              >
                <span>Bắt đầu xác minh</span>
                <ChevronRight size={16} />
              </button>
            </div>
          )}

          {/* ── Step 2: Front ID ── */}
          {step === 2 && (
            <div className="flex flex-col items-center gap-4 animate-fade-in">
              <div className="flex items-center gap-2 text-primary">
                <IdCard size={20} />
                <h2 className="text-lg font-semibold text-slate-900 uppercase tracking-wider">Mặt trước CCCD</h2>
              </div>
              <EkycCamera
                overlayType="id"
                hint="Đặt mặt trước CCCD vào trong khung ngắm. Giữ thẳng và rõ nét."
                onCapture={handleCapture('front', 3)}
                mirrored={false}
              />
              <button onClick={() => setStep(1)} className="text-slate-500 hover:text-primary text-xs font-semibold uppercase tracking-wider transition-colors cursor-pointer bg-transparent border-none">
                ← Quay lại
              </button>
            </div>
          )}

          {/* ── Step 3: Back ID ── */}
          {step === 3 && (
            <div className="flex flex-col items-center gap-4 animate-fade-in">
              <div className="flex items-center gap-2 text-primary">
                <IdCard size={20} />
                <h2 className="text-lg font-semibold text-slate-900 uppercase tracking-wider">Mặt sau CCCD</h2>
              </div>
              {/* Show front preview */}
              <div className="flex justify-center">
                <ImagePreviewCard label="Mặt trước ✓" previewUrl={previews.front} />
              </div>
              <EkycCamera
                overlayType="id"
                hint="Lật thẻ và đặt mặt sau CCCD vào trong khung ngắm."
                onCapture={handleCapture('back', 4)}
                mirrored={false}
              />
              <button onClick={() => setStep(2)} className="text-slate-500 hover:text-primary text-xs font-semibold uppercase tracking-wider transition-colors cursor-pointer bg-transparent border-none">
                ← Quay lại
              </button>
            </div>
          )}

          {/* ── Step 4: Face liveness enrollment ── */}
          {step === 4 && (
            <div className="flex flex-col items-center gap-4 animate-fade-in">
              <div className="flex items-center gap-2 text-primary">
                <Camera size={20} />
                <h2 className="text-lg font-semibold text-slate-900 uppercase tracking-wider">Đăng ký liveness khuôn mặt</h2>
              </div>
              {/* Show both ID previews */}
              <div className="flex gap-4 justify-center">
                <ImagePreviewCard label="Mặt trước ✓" previewUrl={previews.front} />
                <ImagePreviewCard label="Mặt sau ✓" previewUrl={previews.back} />
              </div>
              <EkycLivenessCamera onComplete={handleLivenessComplete} />
              <button onClick={() => setStep(3)} className="text-slate-500 hover:text-primary text-xs font-semibold uppercase tracking-wider transition-colors cursor-pointer bg-transparent border-none">
                ← Quay lại
              </button>
            </div>
          )}

          {/* ── Step 5: Loading ── */}
          {step === 5 && (
            <div className="flex flex-col items-center justify-center gap-6 py-10 animate-fade-in">
              <div className="relative">
                <div className="w-16 h-16 rounded-none border-2 border-primary/20 border-t-primary animate-spin" />
                <ShieldCheck size={24} className="absolute inset-0 m-auto text-primary" />
              </div>
              <div className="text-center">
                <h2 className="text-lg font-semibold text-slate-900 uppercase tracking-wider mb-2">Đang xác minh eKYC</h2>
                <p className="text-slate-500 text-xs font-semibold uppercase tracking-wider leading-relaxed max-w-xs">
                  Hệ thống xác minh CCCD, kiểm tra active/passive liveness và tạo mẫu khuôn mặt từ nhiều góc độ.
                </p>
              </div>
              <div className="w-full max-w-sm rounded-none border border-slate-200 bg-slate-50 p-4 text-left flex flex-col gap-2.5">
                <p className="text-xs text-primary font-semibold uppercase tracking-wide flex items-center gap-2">
                  <CheckCircle2 size={14} /> 1. Đọc số CCCD, họ tên và ngày sinh
                </p>
                <p className="text-xs text-emerald-600 font-semibold uppercase tracking-wide flex items-center gap-2">
                  <CheckCircle2 size={14} /> 2. Kiểm tra người thật và đăng ký khuôn mặt nhiều góc
                </p>
              </div>
              <div className="flex gap-4 justify-center">
                <ImagePreviewCard label="Mặt trước" previewUrl={previews.front} />
                <ImagePreviewCard label="Mặt sau" previewUrl={previews.back} />
                <ImagePreviewCard label="Chính diện" previewUrl={previews.faceFrames?.center} />
              </div>
            </div>
          )}

          {/* ── Step 6: Result ── */}
          {step === 6 && result && (
            <div className="flex flex-col items-center gap-6 py-6 text-center animate-fade-in">
              {result.success ? (
                <>
                  <div className="w-16 h-16 rounded-none bg-emerald-50 border border-emerald-100 flex items-center justify-center text-emerald-600 shadow-sm animate-bounce-once">
                    <CheckCircle2 size={32} />
                  </div>
                  <div>
                    <h2 className="text-xl font-semibold text-slate-900 uppercase tracking-wider mb-2">Xác minh thành công!</h2>
                    <p className="text-slate-500 text-xs font-semibold uppercase tracking-wider leading-relaxed max-w-xs">{result.message}</p>
                  </div>
                  <div className="flex gap-4 justify-center">
                    <ImagePreviewCard label="Mặt trước" previewUrl={previews.front} />
                    <ImagePreviewCard label="Mặt sau" previewUrl={previews.back} />
                    <ImagePreviewCard label="Chính diện" previewUrl={previews.faceFrames?.center} />
                  </div>
                  <button
                    onClick={onClose}
                    className="w-full py-3.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold uppercase text-xs tracking-widest transition-all duration-300 cursor-pointer border-none flex items-center justify-center rounded-none"
                  >
                    Hoàn tất
                  </button>
                </>
              ) : (
                <>
                  <div className="w-16 h-16 rounded-none bg-red-50 border border-red-100 flex items-center justify-center text-red-600 shadow-sm">
                    <XCircle size={32} />
                  </div>
                  <div className="w-full text-center">
                    <h2 className="mb-2 text-xl font-semibold tracking-wide text-slate-900">Chưa thể xác minh danh tính</h2>
                    <p className="mx-auto max-w-sm text-sm leading-relaxed text-slate-500">
                      Hãy kiểm tra các thông tin dưới đây, sau đó chụp lại ảnh CCCD rõ nét và đầy đủ.
                    </p>
                  </div>
                  <div className="w-full rounded-none border border-red-200 bg-red-50 p-4 text-left">
                    <div className="flex items-center gap-2 text-red-800">
                      <AlertCircle size={17} className="shrink-0" />
                      <span className="text-sm font-semibold">Thông tin cần khắc phục</span>
                    </div>
                    <ul className="mt-3 space-y-2 pl-5 text-sm leading-relaxed text-red-700 marker:text-red-400 list-disc">
                      {errorDetails.map((detail, index) => (
                        <li key={`${detail}-${index}`}>{detail}</li>
                      ))}
                    </ul>
                  </div>
                  <div className="flex gap-3 w-full">
                    <button
                      onClick={handleRetry}
                      className="flex-1 py-3.5 bg-primary hover:bg-slate-950 text-white font-semibold uppercase text-xs tracking-widest flex items-center justify-center gap-1.5 transition-all duration-300 cursor-pointer border-none rounded-none parallelogram-btn"
                    >
                      <RotateCcw size={14} />
                      <span>Thử lại</span>
                    </button>
                    <button
                      onClick={onClose}
                      className="flex-1 py-3.5 bg-slate-100 border border-slate-200 text-slate-600 font-semibold uppercase text-xs tracking-widest hover:bg-slate-200 transition-all cursor-pointer rounded-none"
                    >
                      Đóng
                    </button>
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
