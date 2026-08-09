import { useState, useEffect, useCallback } from 'react';
import {
  ShieldCheck, ShieldAlert, ShieldX, Clock, RefreshCcw,
  CheckCircle2, Loader2,
  ArrowLeft
} from 'lucide-react';
import { getEkycDocument, getEkycProfile } from '../../services/ekycService';

// Status badge component
function StatusBadge({ status }) {
  const map = {
    VERIFIED: { label: 'ĐÃ XÁC MINH', icon: CheckCircle2, cls: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' },
    SUBMITTED: { label: 'ĐÃ NỘP HỒ SƠ', icon: Clock, cls: 'bg-amber-500/10 text-amber-400 border-amber-500/20' },
    AI_CHECKING: { label: 'AI ĐANG KIỂM TRA', icon: Clock, cls: 'bg-amber-500/10 text-amber-400 border-amber-500/20' },
    NOT_FOUND: { label: 'CHƯA XÁC MINH', icon: ShieldAlert, cls: 'bg-white/5 text-white/40 border-white/10' },
  };
  const cfg = map[status] || map.NOT_FOUND;
  const Icon = cfg.icon;
  return (
    <span className={`inline-flex items-center gap-1.5 px-3 py-1 border text-[9px] font-black tracking-widest uppercase rounded-none ${cfg.cls}`}>
      <Icon size={10} />
      {cfg.label}
    </span>
  );
}

// Info row
function InfoRow({ label, value }) {
  return (
    <div className="border-b border-white/5 pb-3.5">
      <span className="text-[9px] uppercase tracking-widest text-white/40 font-bold block mb-1.5">{label}</span>
      <span className="text-white text-xs font-black uppercase tracking-wider">{value || '—'}</span>
    </div>
  );
}

// Uploaded image placeholder / component (UC-31 EXC-01)
export function EkycImage({ label, userId, type, onImageError }) {
  const [src, setSrc] = useState(null);
  const [loading, setLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    let objectUrl = null;
    setSrc(null);
    setHasError(false);
    setLoading(true);

    if (!userId) {
      setLoading(false);
      setHasError(true);
      onImageError?.();
      return undefined;
    }

    getEkycDocument(userId, type)
      .then((response) => {
        objectUrl = URL.createObjectURL(response.data);
        setSrc(objectUrl);
      })
      .catch(() => {
        setHasError(true);
        onImageError?.();
      })
      .finally(() => setLoading(false));

    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [userId, type, onImageError]);

  const handleError = () => {
    setHasError(true);
    onImageError?.();
  };

  return (
    <div className="flex flex-col gap-2.5">
      <span className="text-[9px] uppercase tracking-widest text-white/40 font-bold">{label}</span>
      <div className="w-full aspect-[4/3] bg-white/5 border border-white/10 rounded-none overflow-hidden flex items-center justify-center relative hover:border-white/20 transition-colors shadow-inner select-none">
        {loading ? (
          <Loader2 size={18} className="text-white/40 animate-spin" />
        ) : hasError || !src ? (
          <div className="flex flex-col items-center gap-1.5 p-4 text-center">
            <div className="w-8 h-8 rounded-none bg-red-500/10 flex items-center justify-center border border-red-500/20">
              <ShieldX size={16} className="text-red-400" />
            </div>
            <span className="text-[9px] uppercase tracking-widest font-black text-red-300">Image Not Found</span>
            <span className="text-[8px] text-neutral-500 uppercase tracking-wider font-bold">Vui lòng cập nhật eKYC</span>
          </div>
        ) : (
          <>
            <img
              src={src}
              alt={label}
              onError={handleError}
              onContextMenu={(e) => e.preventDefault()}
              onDragStart={(e) => e.preventDefault()}
              className="w-full h-full object-cover transition-transform duration-300 hover:scale-105 select-none"
            />
            {/* Watermark bảo mật đè lên ảnh */}
            <div 
              className="absolute inset-0 pointer-events-none flex items-center justify-center bg-black/10 select-none"
              onContextMenu={(e) => e.preventDefault()}
            >
              <span className="text-[8px] font-black uppercase tracking-widest text-white/25 -rotate-12 select-none">
                HOTEL SMART eKYC • BẢO MẬT
              </span>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

const STATUS_STEPS = [
  { key: 'SUBMITTED', label: 'ĐÃ NỘP HỒ SƠ', desc: 'Hồ sơ eKYC đã được gửi thành công.' },
  { key: 'AI_CHECKING', label: 'AI ĐANG TRÍCH XUẤT DỮ LIỆU', desc: 'AI đang đọc CCCD và đối chiếu với số đã đăng ký.' },
  { key: 'VERIFIED', label: 'XÁC MINH THÀNH CÔNG', desc: 'Đã hoàn tất xác minh eKYC.' },
];

function StatusTimeline({ status }) {
  const stepIndex = {
    NOT_FOUND: -1,
    SUBMITTED: 0,
    AI_CHECKING: 1,
    VERIFIED: 2,
  }[status] ?? 0;

  return (
    <div className="flex flex-col gap-0">
        {STATUS_STEPS.map((step, idx) => {
          const done = idx < stepIndex;
          const active = idx === stepIndex;
          const verifiedActive = active && status === 'VERIFIED';
          const stepComplete = done || verifiedActive;

          return (
            <div key={step.key} className="flex gap-4">
              <div className="flex flex-col items-center">
                <div className={`
                  w-6 h-6 rounded-none flex items-center justify-center flex-shrink-0 border text-[9px] font-black transition-all duration-300
                  ${stepComplete ? 'bg-emerald-500/20 border-emerald-500 text-emerald-400' :
                    active ? 'bg-amber-500/20 border-amber-500 text-amber-400' : 
                    'bg-white/5 border-white/10 text-white/30'}
                `}>
                  {stepComplete ? '✓' : active ? '…' : idx + 1}
                </div>
                {idx < STATUS_STEPS.length - 1 && (
                  <div className={`w-px flex-1 my-1 ${done ? 'bg-emerald-500' : 'bg-white/10'} min-h-[1.5rem]`} />
                )}
              </div>

              <div className={`pb-4 flex-1 ${idx === STATUS_STEPS.length - 1 ? 'pb-0' : ''}`}>
                <p className={`text-[10px] font-black tracking-widest uppercase ${stepComplete ? 'text-emerald-400' : active ? 'text-amber-400' : 'text-white/30'}`}>
                  {step.label}
                  {active && !verifiedActive && <span className="ml-2 text-[8px] bg-amber-500/20 text-amber-400 px-2 py-0.5 animate-pulse font-bold tracking-widest">ĐANG XỬ LÝ</span>}
                </p>
                <p className={`text-[9px] font-bold uppercase tracking-wider mt-1 ${stepComplete || active ? 'text-white/50' : 'text-white/25'}`}>
                  {step.desc}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    );
  }

export default function ViewEkyc({ onBack, onRegister, onUpdate }) {
  const [ekycData, setEkycData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [hasImageError, setHasImageError] = useState(false);
  const [showStatusPopup, setShowStatusPopup] = useState(false);

  const handleImageError = useCallback(() => {
    setHasImageError(true);
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    setHasImageError(false);
    try {
      const token = localStorage.getItem('accessToken');
      if (!token) throw new Error('Bạn chưa đăng nhập.');
      const res = await getEkycProfile();
      setEkycData(res?.data || null);
    } catch (err) {
      if (err?.response?.status === 404) {
        setEkycData(null); // No eKYC yet
      } else {
        setError(err?.response?.data?.message || err.message || 'Không thể tải dữ liệu eKYC.');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void Promise.resolve().then(load);
  }, [load]);

  const status = ekycData?.status || 'NOT_FOUND';

  // Get status custom styles
  const getStatusStyles = () => {
    switch (status) {
      case 'VERIFIED':
        return {
          border: 'border-l-emerald-500',
          title: 'ĐÃ XÁC MINH DANH TÍNH',
          desc: 'ĐÃ KÍCH HOẠT FACEID EXPRESS CHECK-IN',
          icon: <ShieldCheck className="text-emerald-500 size-7" />,
        };
      case 'SUBMITTED':
        return {
          border: 'border-l-amber-500',
          title: 'ĐÃ NỘP HỒ SƠ',
          desc: 'Hồ sơ eKYC đã được gửi thành công.',
          icon: <Clock className="text-amber-500 size-7" />,
        };
      case 'AI_CHECKING':
        return {
          border: 'border-l-amber-500',
          title: 'AI ĐANG TRÍCH XUẤT DỮ LIỆU',
          desc: 'AI đang đọc CCCD và đối chiếu với số CCCD đã đăng ký.',
          icon: <Clock className="text-amber-500 size-7 animate-pulse" />,
        };
      case 'NOT_FOUND':
      default:
        return {
          border: 'border-l-primary',
          title: 'CHƯA XÁC MINH DANH TÍNH',
          desc: 'Hãy hoàn thành eKYC để kích hoạt FaceID và check-in không cần quầy lễ tân.',
          icon: <ShieldAlert className="text-neutral-500 size-7" />,
        };
    }
  };

  const currentStyles = getStatusStyles();

  return (
    <div className="max-w-2xl mx-auto p-4 md:p-6 animate-fade-in font-['Montserrat']">
      {/* Header */}
      <div className="flex items-center gap-4 mb-8 border-b border-neutral-900 pb-5">
        <button onClick={onBack} className="p-3 bg-[#111111] border border-neutral-800 rounded-none text-slate-400 hover:text-white hover:bg-white/5 transition-all cursor-pointer">
          <ArrowLeft size={16} />
        </button>
        <div>
          <h1 className="text-base font-black font-['Montserrat'] tracking-[0.15em] text-slate-900 uppercase">XÁC MINH DANH TÍNH (E-KYC)</h1>
          <p className="text-slate-500 text-[8.5px] uppercase tracking-widest font-bold mt-1.5 opacity-80">Quản lý xác minh danh tính điện tử</p>
        </div>
      </div>

      {/* Loading state */}
      {loading && (
        <div className="bg-[#111111] border border-neutral-800 rounded-none p-16 flex flex-col items-center justify-center gap-4">
          <Loader2 size={36} className="text-primary animate-spin" />
          <p className="text-neutral-500 text-[9px] uppercase tracking-widest font-bold">Đang tải thông tin hồ sơ...</p>
        </div>
      )}

      {/* Error state */}
      {!loading && error && (
        <div className="bg-[#111111] border border-neutral-800 border-l-4 border-l-red-500 rounded-none p-8 flex flex-col items-center gap-6">
          <ShieldX size={36} className="text-red-400" />
          <p className="text-red-200 text-xs text-center font-bold uppercase tracking-widest leading-relaxed">{error}</p>
          <button onClick={load} className="flex items-center gap-2 px-6 py-3 border border-red-500/30 bg-red-500/10 text-red-400 hover:bg-red-500/20 text-[9px] font-bold uppercase tracking-widest transition-all cursor-pointer rounded-none">
            <RefreshCcw size={12} /> Thử lại
          </button>
        </div>
      )}

      {/* Main Container */}
      {!loading && !error && (
        <div className={`p-8 md:p-10 bg-[#111111] rounded-none border border-neutral-800 border-l-4 ${currentStyles.border} shadow-xl relative overflow-hidden flex flex-col gap-8`}>
          {/* Backdrop pattern */}
          <div className="absolute size-60 bg-primary rounded-full blur-[80px] opacity-10 -right-20 -top-20 pointer-events-none" />

          {/* Top Row: Title & Icon */}
          <div className="relative z-10 flex justify-between items-start">
            <div className="space-y-2.5">
              <h2 className="text-white font-black text-lg md:text-xl uppercase tracking-wider leading-relaxed pr-4">
                {currentStyles.title}
              </h2>
              <p className="text-neutral-400 text-xs font-medium leading-relaxed uppercase tracking-wider max-w-lg">
                {currentStyles.desc}
              </p>
              <StatusBadge status={status} />
            </div>
            {currentStyles.icon}
          </div>



          {/* Info grid & Photos Section for active profile */}
          {status !== 'NOT_FOUND' && (
            <div className="space-y-8 relative z-10 pt-4 border-t border-white/10">
              {/* Profile fields info grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <InfoRow label="Họ và tên" value={ekycData?.fullName} />
                <InfoRow label="Số CCCD/CMND" value={ekycData?.idNumber} />
                <InfoRow label="Ngày sinh" value={ekycData?.dateOfBirth} />
                <InfoRow label="Giới tính" value={ekycData?.gender} />
                <InfoRow label="Quê quán (suy từ CCCD)" value={ekycData?.hometown || ekycData?.provinceName} />
                <InfoRow
                  label="Mã tỉnh CCCD"
                  value={ekycData?.provinceCode}
                />
              </div>

              {/* Uploaded Documents */}
              <div className="space-y-4">
                <h3 className="text-white font-bold text-xs uppercase tracking-widest border-b border-white/5 pb-2">Tài liệu đã tải lên</h3>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <EkycImage label="Mặt trước CCCD" userId={ekycData?.userId} type="front" onImageError={handleImageError} />
                  <EkycImage label="Mặt sau CCCD" userId={ekycData?.userId} type="back" onImageError={handleImageError} />
                  <EkycImage label="Ảnh chân dung (Selfie)" userId={ekycData?.userId} type="face" onImageError={handleImageError} />
                </div>
              </div>

              {/* Image Load Error Alert Box */}
              {hasImageError && (
                <div className="bg-red-500/10 border border-red-500/20 rounded-none p-5 flex items-start gap-3">
                  <ShieldAlert size={16} className="text-red-400 flex-shrink-0 mt-0.5 animate-pulse" />
                  <div>
                    <p className="text-red-300 text-xs font-bold uppercase tracking-wider mb-1">Phát hiện hình ảnh lỗi (EXC-01)</p>
                    <p className="text-red-400/80 text-[10px] uppercase font-bold tracking-wider leading-relaxed">
                      Hệ thống không thể tải một số hình ảnh. Vui lòng cập nhật eKYC để gửi lại tài liệu mới.
                    </p>
                  </div>
                </div>
              )}

            </div>
          )}

          {/* Action Row */}
          <div className="relative z-10 border-t border-white/10 pt-6 mt-2 flex flex-col gap-4">
            {/* If NOT_FOUND, register button */}
            {status === 'NOT_FOUND' && (
              <button
                onClick={onRegister}
                className="w-full py-4 bg-primary hover:bg-white hover:text-black text-white font-black uppercase text-[10px] tracking-[0.15em] transition-all duration-300 cursor-pointer border-none flex items-center justify-center gap-2 parallelogram-btn rounded-none"
              >
                <ShieldCheck size={16} />
                <span>Xác minh eKYC ngay</span>
              </button>
            )}

            {/* Bottom verified date string (matching image) */}
            {status === 'VERIFIED' && ekycData?.verifiedAt && (
              <p className="text-white/30 text-[9px] uppercase tracking-widest font-black text-center mt-1">
                ĐÃ XÁC MINH VÀO {new Date(ekycData.verifiedAt).toLocaleDateString('vi-VN')}
              </p>
            )}
          </div>
        </div>
      )}
 
      {/* Status Popup Modal (UC-33) */}
      {showStatusPopup && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 animate-fade-in"
          style={{ background: 'rgba(0,0,0,0.85)', backdropFilter: 'blur(6px)' }}>
          <div className="
            relative w-full max-w-md bg-[#111111]
            rounded-none border border-neutral-800 p-8 shadow-2xl
            flex flex-col gap-6
          ">
            {/* Close button */}
            <button
              onClick={() => setShowStatusPopup(false)}
              className="absolute top-4 right-4 text-neutral-500 hover:text-white text-[9px] font-black tracking-widest uppercase border-none bg-transparent cursor-pointer"
            >
              ✕ ĐÓNG
            </button>
 
            <div className="flex flex-col gap-1.5 border-b border-white/5 pb-4">
              <h2 className="text-white font-black text-sm uppercase tracking-widest leading-relaxed">
                TIẾN TRÌNH XÁC MINH (UC-33)
              </h2>
              <p className="text-neutral-500 text-[8px] uppercase tracking-wider font-bold">
                Theo dõi trạng thái xử lý chi tiết eKYC
              </p>
            </div>
 
            <StatusTimeline status={status} />
          </div>
        </div>
      )}
    </div>
  );
}
