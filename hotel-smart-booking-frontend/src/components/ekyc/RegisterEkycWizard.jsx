import { useState, useCallback } from 'react';
import {
  ShieldCheck, IdCard, Camera, CheckCircle2, XCircle,
  ChevronRight, RotateCcw, AlertCircle,
} from 'lucide-react';
import EkycCamera from './EkycCamera';

// ─── Step indicator bar ───────────────────────────────────────────────────────
const STEPS = [
  { id: 1, label: 'Giới thiệu' },
  { id: 2, label: 'Mặt trước' },
  { id: 3, label: 'Mặt sau' },
  { id: 4, label: 'Selfie' },
  { id: 5, label: 'Xác minh' },
  { id: 6, label: 'Kết quả' },
];

function StepBar({ current }) {
  return (
    <div className="flex items-center justify-center gap-1 mb-8">
      {STEPS.map((s, idx) => {
        const done = s.id < current;
        const active = s.id === current;
        return (
          <div key={s.id} className="flex items-center gap-1">
            <div className={`
              flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold font-['Geist'] transition-all duration-300
              ${done ? 'bg-emerald-500 text-white' : active ? 'bg-amber-400 text-slate-900' : 'bg-white/10 text-slate-500'}
            `}>
              {done ? <CheckCircle2 size={14} /> : s.id}
            </div>
            {idx < STEPS.length - 1 && (
              <div className={`w-6 h-0.5 rounded-full transition-all duration-500 ${done ? 'bg-emerald-500' : 'bg-white/10'}`} />
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
    <div className="flex flex-col items-center gap-2">
      <span className="text-xs text-slate-400 font-['Geist'] uppercase tracking-wider">{label}</span>
      <img
        src={previewUrl}
        alt={label}
        className="w-28 h-20 object-cover rounded-xl border-2 border-emerald-500/40 shadow-lg"
      />
    </div>
  );
}

// ─── Main RegisterEkyc wizard ─────────────────────────────────────────────────
/**
 * @param {(front: File, back: File, selfie: File) => Promise<void>} onSubmit
 * @param {() => void} onClose
 * @param {'register' | 'update'} mode
 */
export default function RegisterEkycWizard({ onSubmit, onClose, mode = 'register' }) {
  const [step, setStep] = useState(1);
  const [images, setImages] = useState({ front: null, back: null, selfie: null });
  const [previews, setPreviews] = useState({ front: null, back: null, selfie: null });
  const [result, setResult] = useState(null); // { success, message }
  const [loading, setLoading] = useState(false);

  const handleCapture = useCallback((key, nextStep) => (file, previewUrl) => {
    setImages(prev => ({ ...prev, [key]: file }));
    setPreviews(prev => ({ ...prev, [key]: previewUrl }));
    setStep(nextStep);
  }, []);

  const handleSelfieCapture = useCallback(async (file, previewUrl) => {
    setImages(prev => ({ ...prev, selfie: file }));
    setPreviews(prev => ({ ...prev, selfie: previewUrl }));
    setStep(5);

    setLoading(true);
    try {
      const res = await onSubmit(images.front, images.back, file);
      const successMessage = res?.data?.message || res?.message || 'Xác minh danh tính thành công! Tài khoản của bạn đã được kích hoạt đầy đủ.';
      setResult({ success: true, message: successMessage });
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Xác minh thất bại. Vui lòng thử lại.';
      setResult({ success: false, message: msg });
    } finally {
      setLoading(false);
      setStep(6);
    }
  }, [images.front, images.back, onSubmit]);

  const handleRetry = () => {
    setStep(1);
    setImages({ front: null, back: null, selfie: null });
    setPreviews({ front: null, back: null, selfie: null });
    setResult(null);
    setLoading(false);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(0,0,0,0.80)', backdropFilter: 'blur(8px)' }}>
      <div className="
        relative w-full max-w-lg bg-gradient-to-b from-slate-800 to-slate-900
        rounded-3xl shadow-2xl shadow-black/60
        border border-white/10 overflow-hidden
        flex flex-col
      ">
        {/* Close button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 z-10 w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-slate-400 hover:text-white transition-all"
        >
          ✕
        </button>

        {/* Scrollable body */}
        <div className="overflow-y-auto max-h-[90vh] p-6 sm:p-8 flex flex-col gap-6">
          <StepBar current={step} />

          {/* ── Step 1: Introduction ── */}
          {step === 1 && (
            <div className="flex flex-col items-center gap-6 text-center animate-fade-in">
              <div className="w-20 h-20 rounded-full bg-gradient-to-br from-amber-400 to-yellow-300 flex items-center justify-center shadow-lg shadow-amber-500/30">
                <ShieldCheck size={38} className="text-slate-900" />
              </div>
              <div>
                <h2 className="text-2xl font-bold font-['Playfair_Display'] text-white mb-2">
                  {mode === 'update' ? 'Cập nhật eKYC' : 'Xác minh danh tính'}
                </h2>
                <p className="text-slate-400 text-sm font-['Geist'] leading-relaxed">
                  Quy trình xác minh danh tính điện tử giúp bảo vệ tài khoản của bạn.
                </p>
              </div>

              <div className="w-full bg-white/5 border border-white/10 rounded-2xl p-5 text-left flex flex-col gap-3">
                <p className="text-amber-300 text-sm font-semibold font-['Geist']">📋 Chuẩn bị trước khi bắt đầu:</p>
                {[
                  '🪪 CCCD/CMND còn hiệu lực và rõ ràng',
                  '💡 Chọn nơi đủ sáng, tránh ngược sáng',
                  '😶 Không đeo kính, khẩu trang khi chụp selfie',
                  '📱 Đặt thẻ phẳng, không che khuất thông tin',
                ].map((tip, i) => (
                  <p key={i} className="text-slate-300 text-sm font-['Geist']">{tip}</p>
                ))}
              </div>

              <button
                onClick={() => setStep(2)}
                className="w-full py-4 rounded-2xl bg-gradient-to-r from-amber-400 to-yellow-300 text-slate-900 font-bold font-['Geist'] flex items-center justify-center gap-2 hover:shadow-lg hover:shadow-amber-500/30 hover:scale-[1.02] transition-all duration-200"
              >
                <span>Bắt đầu xác minh</span>
                <ChevronRight size={18} />
              </button>
            </div>
          )}

          {/* ── Step 2: Front ID ── */}
          {step === 2 && (
            <div className="flex flex-col items-center gap-4 animate-fade-in">
              <div className="flex items-center gap-2 text-amber-300">
                <IdCard size={22} />
                <h2 className="text-xl font-bold font-['Playfair_Display'] text-white">Mặt trước CCCD</h2>
              </div>
              <EkycCamera
                overlayType="id"
                hint="Đặt mặt trước CCCD vào trong khung vàng. Giữ thẳng và rõ nét."
                onCapture={handleCapture('front', 3)}
                mirrored={false}
              />
              <button onClick={() => setStep(1)} className="text-slate-500 hover:text-slate-300 text-sm font-['Geist'] transition-colors">
                ← Quay lại
              </button>
            </div>
          )}

          {/* ── Step 3: Back ID ── */}
          {step === 3 && (
            <div className="flex flex-col items-center gap-4 animate-fade-in">
              <div className="flex items-center gap-2">
                <IdCard size={22} className="text-amber-300" />
                <h2 className="text-xl font-bold font-['Playfair_Display'] text-white">Mặt sau CCCD</h2>
              </div>
              {/* Show front preview */}
              <div className="flex justify-center">
                <ImagePreviewCard label="Mặt trước ✓" previewUrl={previews.front} />
              </div>
              <EkycCamera
                overlayType="id"
                hint="Lật thẻ và đặt mặt sau CCCD vào trong khung vàng."
                onCapture={handleCapture('back', 4)}
                mirrored={false}
              />
              <button onClick={() => setStep(2)} className="text-slate-500 hover:text-slate-300 text-sm font-['Geist'] transition-colors">
                ← Quay lại
              </button>
            </div>
          )}

          {/* ── Step 4: Selfie ── */}
          {step === 4 && (
            <div className="flex flex-col items-center gap-4 animate-fade-in">
              <div className="flex items-center gap-2">
                <Camera size={22} className="text-emerald-400" />
                <h2 className="text-xl font-bold font-['Playfair_Display'] text-white">Chụp chân dung</h2>
              </div>
              {/* Show both ID previews */}
              <div className="flex gap-4 justify-center">
                <ImagePreviewCard label="Mặt trước ✓" previewUrl={previews.front} />
                <ImagePreviewCard label="Mặt sau ✓" previewUrl={previews.back} />
              </div>
              <EkycCamera
                overlayType="selfie"
                hint="Đưa khuôn mặt vào trong khung bầu dục xanh. Nhìn thẳng vào camera."
                onCapture={handleSelfieCapture}
                mirrored={true}
              />
              <button onClick={() => setStep(3)} className="text-slate-500 hover:text-slate-300 text-sm font-['Geist'] transition-colors">
                ← Quay lại
              </button>
            </div>
          )}

          {/* ── Step 5: Loading ── */}
          {step === 5 && (
            <div className="flex flex-col items-center justify-center gap-6 py-10 animate-fade-in">
              <div className="relative">
                <div className="w-24 h-24 rounded-full border-4 border-amber-400/20 border-t-amber-400 animate-spin" />
                <ShieldCheck size={32} className="absolute inset-0 m-auto text-amber-400" />
              </div>
              <div className="text-center">
                <h2 className="text-xl font-bold font-['Playfair_Display'] text-white mb-2">Đang xử lý</h2>
                <p className="text-slate-400 text-sm font-['Geist'] leading-relaxed max-w-xs">
                  Hệ thống AI đang đối chiếu thông tin, vui lòng đợi...
                </p>
              </div>
              <div className="flex gap-4">
                <ImagePreviewCard label="Mặt trước" previewUrl={previews.front} />
                <ImagePreviewCard label="Mặt sau" previewUrl={previews.back} />
                <ImagePreviewCard label="Selfie" previewUrl={previews.selfie} />
              </div>
            </div>
          )}

          {/* ── Step 6: Result ── */}
          {step === 6 && result && (
            <div className="flex flex-col items-center gap-6 py-6 text-center animate-fade-in">
              {result.success ? (
                <>
                  <div className="w-24 h-24 rounded-full bg-gradient-to-br from-emerald-500 to-green-400 flex items-center justify-center shadow-xl shadow-emerald-500/30 animate-bounce-once">
                    <CheckCircle2 size={48} className="text-white" />
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold font-['Playfair_Display'] text-white mb-2">Xác minh thành công!</h2>
                    <p className="text-slate-400 text-sm font-['Geist'] leading-relaxed max-w-xs">{result.message}</p>
                  </div>
                  <div className="flex gap-4">
                    <ImagePreviewCard label="Mặt trước" previewUrl={previews.front} />
                    <ImagePreviewCard label="Mặt sau" previewUrl={previews.back} />
                    <ImagePreviewCard label="Selfie" previewUrl={previews.selfie} />
                  </div>
                  <button
                    onClick={onClose}
                    className="w-full py-4 rounded-2xl bg-gradient-to-r from-emerald-500 to-green-400 text-white font-bold font-['Geist'] hover:shadow-lg hover:shadow-emerald-500/30 hover:scale-[1.02] transition-all duration-200"
                  >
                    Hoàn tất
                  </button>
                </>
              ) : (
                <>
                  <div className="w-24 h-24 rounded-full bg-gradient-to-br from-red-600 to-rose-500 flex items-center justify-center shadow-xl shadow-red-500/30">
                    <XCircle size={48} className="text-white" />
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold font-['Playfair_Display'] text-white mb-2">Xác minh thất bại</h2>
                    <div className="flex items-start gap-2 bg-red-500/10 border border-red-500/20 rounded-xl p-4 text-left">
                      <AlertCircle size={16} className="text-red-400 flex-shrink-0 mt-0.5" />
                      <p className="text-red-300 text-sm font-['Geist'] leading-relaxed">{result.message}</p>
                    </div>
                  </div>
                  <div className="flex gap-3 w-full">
                    <button
                      onClick={handleRetry}
                      className="flex-1 py-4 rounded-2xl bg-gradient-to-r from-amber-400 to-yellow-300 text-slate-900 font-bold font-['Geist'] flex items-center justify-center gap-2 hover:shadow-lg hover:shadow-amber-500/30 hover:scale-[1.02] transition-all duration-200"
                    >
                      <RotateCcw size={16} />
                      <span>Thử lại</span>
                    </button>
                    <button
                      onClick={onClose}
                      className="flex-1 py-4 rounded-2xl bg-white/10 border border-white/10 text-slate-300 font-semibold font-['Geist'] hover:bg-white/20 transition-all"
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
