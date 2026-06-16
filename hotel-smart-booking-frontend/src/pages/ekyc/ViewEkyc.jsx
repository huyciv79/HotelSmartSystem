import { useState, useEffect, useCallback } from 'react';
import {
  ShieldCheck, ShieldAlert, ShieldX, Clock, RefreshCcw,
  FileText, CheckCircle2, XCircle, Loader2, ExternalLink,
  ArrowLeft, User, Calendar, CreditCard,
} from 'lucide-react';
import { getEkycProfile } from '../../services/ekycService';

// Status badge component
function StatusBadge({ status }) {
  const map = {
    VERIFIED:  { label: 'Đã xác minh',   icon: CheckCircle2, cls: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30' },
    PENDING:   { label: 'Chờ duyệt',      icon: Clock,         cls: 'bg-amber-500/15  text-amber-400  border-amber-500/30'  },
    REJECTED:  { label: 'Bị từ chối',     icon: XCircle,       cls: 'bg-red-500/15    text-red-400    border-red-500/30'    },
    NOT_FOUND: { label: 'Chưa đăng ký',   icon: ShieldAlert,   cls: 'bg-slate-500/15  text-slate-400  border-slate-500/30'  },
  };
  const cfg = map[status] || map.NOT_FOUND;
  const Icon = cfg.icon;
  return (
    <span className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full border text-xs font-semibold font-['Geist'] ${cfg.cls}`}>
      <Icon size={12} />
      {cfg.label}
    </span>
  );
}

// Info row
function InfoRow({ label, value }) {
  return (
    <div className="flex flex-col gap-0.5">
      <span className="text-xs uppercase tracking-wider text-slate-500 font-['Geist']">{label}</span>
      <span className="text-white text-sm font-medium font-['Geist']">{value || '—'}</span>
    </div>
  );
}

/**
 * @param {() => void} onBack
 * @param {() => void} onRegister
 * @param {() => void} onUpdate
 * @param {() => void} onViewStatus
 */
export default function ViewEkyc({ onBack, onRegister, onUpdate, onViewStatus }) {
  const [ekycData, setEkycData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
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

  useEffect(() => { load(); }, [load]);

  const status = ekycData?.status || 'NOT_FOUND';

  return (
    <div className="flex flex-col gap-6 animate-fade-in max-w-2xl mx-auto">
      {/* Header */}
      <div className="flex items-center gap-3">
        <button onClick={onBack} className="p-2 rounded-full hover:bg-white/10 text-slate-400 hover:text-white transition-all">
          <ArrowLeft size={18} />
        </button>
        <div>
          <h1 className="text-2xl font-bold font-['Playfair_Display'] text-white">eKYC Identity</h1>
          <p className="text-slate-500 text-xs font-['Geist']">Quản lý xác minh danh tính điện tử</p>
        </div>
      </div>

      {/* Loading state */}
      {loading && (
        <div className="bg-slate-800/50 border border-white/10 rounded-2xl p-12 flex flex-col items-center gap-3">
          <Loader2 size={32} className="text-amber-400 animate-spin" />
          <p className="text-slate-500 text-sm font-['Geist']">Đang tải thông tin...</p>
        </div>
      )}

      {/* Error state */}
      {!loading && error && (
        <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-6 flex flex-col items-center gap-4">
          <ShieldX size={32} className="text-red-400" />
          <p className="text-red-300 text-sm font-['Geist'] text-center">{error}</p>
          <button onClick={load} className="flex items-center gap-2 px-4 py-2 rounded-xl bg-red-500/20 text-red-300 hover:bg-red-500/30 text-sm font-['Geist'] transition-all">
            <RefreshCcw size={14} /> Thử lại
          </button>
        </div>
      )}

      {/* No eKYC registered */}
      {!loading && !error && (!ekycData || status === 'NOT_FOUND') && (
        <div className="bg-slate-800/60 border border-white/10 rounded-3xl overflow-hidden">
          {/* Banner */}
          <div className="relative p-8 flex flex-col items-center text-center gap-4"
            style={{ background: 'linear-gradient(135deg, rgba(250,204,21,0.08) 0%, rgba(16,185,129,0.05) 100%)' }}>
            <div className="absolute inset-0 pointer-events-none"
              style={{ background: 'radial-gradient(ellipse 60% 80% at 50% 0%, rgba(250,204,21,0.06), transparent)' }} />
            <div className="relative w-20 h-20 rounded-full bg-gradient-to-br from-slate-700 to-slate-600 border border-white/10 flex items-center justify-center shadow-xl">
              <ShieldAlert size={34} className="text-slate-400" />
            </div>
            <div>
              <h2 className="text-xl font-bold font-['Playfair_Display'] text-white mb-1">Chưa xác minh danh tính</h2>
              <p className="text-slate-400 text-sm font-['Geist'] max-w-sm leading-relaxed">
                Xác minh eKYC giúp kích hoạt đặt phòng ưu tiên, check-in kỹ thuật số và các dịch vụ cao cấp.
              </p>
            </div>
            <StatusBadge status="NOT_FOUND" />
          </div>

          {/* Actions */}
          <div className="p-6 flex flex-col gap-3 border-t border-white/5">
            <button
              onClick={onRegister}
              className="w-full py-4 rounded-2xl bg-gradient-to-r from-amber-400 to-yellow-300 text-slate-900 font-bold font-['Geist'] flex items-center justify-center gap-2 hover:shadow-lg hover:shadow-amber-500/25 hover:scale-[1.01] transition-all duration-200"
            >
              <ShieldCheck size={18} />
              <span>Đăng ký eKYC ngay</span>
            </button>
            <button
              onClick={onViewStatus}
              className="w-full py-3 rounded-2xl bg-white/5 border border-white/10 text-slate-300 font-semibold font-['Geist'] flex items-center justify-center gap-2 hover:bg-white/10 transition-all text-sm"
            >
              <ExternalLink size={14} />
              <span>Xem trạng thái eKYC</span>
            </button>
          </div>
        </div>
      )}

      {/* Has eKYC data */}
      {!loading && !error && ekycData && status !== 'NOT_FOUND' && (
        <div className="flex flex-col gap-4">
          {/* Status card */}
          <div className="bg-slate-800/60 border border-white/10 rounded-3xl overflow-hidden">
            <div className="relative p-6 flex items-center gap-4"
              style={{ background: status === 'VERIFIED' ? 'linear-gradient(135deg, rgba(16,185,129,0.1), transparent)' : status === 'REJECTED' ? 'linear-gradient(135deg, rgba(239,68,68,0.1), transparent)' : 'linear-gradient(135deg, rgba(250,204,21,0.08), transparent)' }}>
              <div className={`w-14 h-14 rounded-2xl flex items-center justify-center shadow-lg ${status === 'VERIFIED' ? 'bg-emerald-500/20' : status === 'REJECTED' ? 'bg-red-500/20' : 'bg-amber-500/20'}`}>
                {status === 'VERIFIED' ? <ShieldCheck size={26} className="text-emerald-400" /> : status === 'REJECTED' ? <ShieldX size={26} className="text-red-400" /> : <Clock size={26} className="text-amber-400" />}
              </div>
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-1">
                  <h3 className="text-white font-bold font-['Playfair_Display'] text-lg">Trạng thái eKYC</h3>
                  <StatusBadge status={status} />
                </div>
                {ekycData.verifiedAt && (
                  <p className="text-slate-500 text-xs font-['Geist']">
                    Xác minh lúc: {new Date(ekycData.verifiedAt).toLocaleString('vi-VN')}
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* Info grid */}
          <div className="bg-slate-800/60 border border-white/10 rounded-3xl p-6 grid grid-cols-2 gap-5">
            <InfoRow label="Họ và tên" value={ekycData.fullName} />
            <InfoRow label="Số CCCD/CMND" value={ekycData.idNumber} />
            <InfoRow label="Ngày sinh" value={ekycData.dateOfBirth} />
            <InfoRow label="Địa chỉ" value={ekycData.address} />
          </div>

          {/* Rejection reason */}
          {status === 'REJECTED' && ekycData.rejectionReason && (
            <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-4 flex items-start gap-2">
              <XCircle size={16} className="text-red-400 flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-red-300 text-sm font-semibold font-['Geist'] mb-0.5">Lý do từ chối:</p>
                <p className="text-red-400/80 text-sm font-['Geist']">{ekycData.rejectionReason}</p>
              </div>
            </div>
          )}

          {/* Action buttons */}
          <div className="flex gap-3">
            {status === 'REJECTED' && (
              <button
                onClick={onUpdate}
                className="flex-1 py-3.5 rounded-2xl bg-gradient-to-r from-amber-400 to-yellow-300 text-slate-900 font-bold font-['Geist'] flex items-center justify-center gap-2 hover:shadow-lg hover:shadow-amber-500/25 hover:scale-[1.01] transition-all duration-200"
              >
                <RefreshCcw size={16} />
                <span>Cập nhật</span>
              </button>
            )}
            <button
              onClick={onViewStatus}
              className="flex-1 py-3.5 rounded-2xl bg-white/5 border border-white/10 text-slate-300 font-semibold font-['Geist'] flex items-center justify-center gap-2 hover:bg-white/10 transition-all text-sm"
            >
              <ExternalLink size={14} />
              <span>Chi tiết trạng thái</span>
            </button>
          </div>
        </div>
      )}

      {/* Footer status link */}
      <div className="border-t border-white/5 pt-4 flex justify-center">
        <button
          onClick={onViewStatus}
          className="flex items-center gap-1.5 text-xs text-slate-500 hover:text-amber-400 font-['Geist'] transition-colors"
        >
          <FileText size={12} />
          <span>eKYC Status — Xem lịch sử xác minh</span>
        </button>
      </div>
    </div>
  );
}
