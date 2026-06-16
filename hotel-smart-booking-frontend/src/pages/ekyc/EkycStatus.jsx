import { useState, useEffect, useCallback } from 'react';
import {
  ArrowLeft, Clock, CheckCircle2, XCircle, Loader2,
  ShieldCheck, ShieldX, ShieldAlert, RefreshCcw, Info,
} from 'lucide-react';
import { getEkycProfile } from '../../services/ekycService';

const STATUS_STEPS = [
  { key: 'SUBMITTED',   label: 'Đã nộp hồ sơ',       desc: 'Ảnh và thông tin đã được gửi thành công.' },
  { key: 'AI_CHECKING', label: 'AI đang đối chiếu',   desc: 'Hệ thống AI đang phân tích và so khớp dữ liệu.' },
  { key: 'REVIEWING',   label: 'Chờ duyệt thủ công',  desc: 'Nhân viên kiểm tra thông tin xác minh.' },
  { key: 'VERIFIED',    label: 'Xác minh thành công', desc: 'Danh tính của bạn đã được xác nhận.' },
];

function StatusTimeline({ status }) {
  // Map API status to step index
  const stepIndex = {
    NOT_FOUND: -1,
    PENDING: 2,
    VERIFIED: 3,
    REJECTED: 3,
  }[status] ?? 0;

  const isRejected = status === 'REJECTED';

  return (
    <div className="flex flex-col gap-0">
      {STATUS_STEPS.map((step, idx) => {
        const done = idx < stepIndex;
        const active = idx === stepIndex && !isRejected;
        const failed = idx === stepIndex && isRejected;

        return (
          <div key={step.key} className="flex gap-4">
            {/* Timeline column */}
            <div className="flex flex-col items-center">
              <div className={`
                w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 border-2 transition-all duration-300
                ${done ? 'bg-emerald-500 border-emerald-500' : active ? 'bg-amber-400 border-amber-400' : failed ? 'bg-red-500 border-red-500' : 'bg-slate-700 border-slate-600'}
              `}>
                {done ? <CheckCircle2 size={14} className="text-white" /> :
                 active ? <div className="w-2 h-2 bg-slate-900 rounded-full animate-pulse" /> :
                 failed ? <XCircle size={14} className="text-white" /> :
                 <div className="w-2 h-2 bg-slate-500 rounded-full" />}
              </div>
              {idx < STATUS_STEPS.length - 1 && (
                <div className={`w-0.5 flex-1 my-1 rounded-full min-h-[2rem] ${done ? 'bg-emerald-500' : 'bg-slate-700'}`} />
              )}
            </div>

            {/* Content */}
            <div className={`pb-6 flex-1 ${idx === STATUS_STEPS.length - 1 ? 'pb-0' : ''}`}>
              <p className={`text-sm font-semibold font-['Geist'] ${done ? 'text-emerald-400' : active ? 'text-amber-300' : failed ? 'text-red-400' : 'text-slate-500'}`}>
                {step.label}
                {active && <span className="ml-2 text-xs bg-amber-400/20 text-amber-400 px-2 py-0.5 rounded-full animate-pulse">Hiện tại</span>}
              </p>
              <p className={`text-xs font-['Geist'] mt-0.5 ${done || active || failed ? 'text-slate-400' : 'text-slate-600'}`}>
                {failed && idx === stepIndex ? 'Xác minh không thành công.' : step.desc}
              </p>
            </div>
          </div>
        );
      })}
    </div>
  );
}

/**
 * @param {() => void} onBack
 */
export default function EkycStatus({ onBack }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const token = localStorage.getItem('accessToken');
      if (!token) throw new Error('Bạn chưa đăng nhập.');
      const res = await getEkycProfile();
      setData(res?.data || null);
    } catch (err) {
      if (err?.response?.status === 404) {
        setData(null);
      } else {
        setError(err?.response?.data?.message || err.message);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const status = data?.status || 'NOT_FOUND';

  return (
    <div className="flex flex-col gap-6 animate-fade-in max-w-lg mx-auto">
      {/* Header */}
      <div className="flex items-center gap-3">
        <button onClick={onBack} className="p-2 rounded-full hover:bg-white/10 text-slate-400 hover:text-white transition-all">
          <ArrowLeft size={18} />
        </button>
        <div>
          <h1 className="text-2xl font-bold font-['Playfair_Display'] text-white">Trạng thái eKYC</h1>
          <p className="text-slate-500 text-xs font-['Geist']">Theo dõi tiến trình xác minh</p>
        </div>
        <button onClick={load} className="ml-auto p-2 rounded-full hover:bg-white/10 text-slate-400 hover:text-white transition-all" title="Làm mới">
          <RefreshCcw size={16} />
        </button>
      </div>

      {/* Loading */}
      {loading && (
        <div className="bg-slate-800/50 border border-white/10 rounded-2xl p-10 flex flex-col items-center gap-3">
          <Loader2 size={28} className="text-amber-400 animate-spin" />
          <p className="text-slate-500 text-sm font-['Geist']">Đang tải...</p>
        </div>
      )}

      {/* Error */}
      {!loading && error && (
        <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-6 text-center">
          <p className="text-red-300 text-sm font-['Geist']">{error}</p>
        </div>
      )}

      {/* No eKYC */}
      {!loading && !error && (!data || status === 'NOT_FOUND') && (
        <div className="bg-slate-800/60 border border-white/10 rounded-3xl p-8 flex flex-col items-center gap-4 text-center">
          <ShieldAlert size={36} className="text-slate-500" />
          <p className="text-slate-400 text-sm font-['Geist']">Bạn chưa đăng ký eKYC. Hãy quay lại và bắt đầu đăng ký.</p>
        </div>
      )}

      {/* Has data */}
      {!loading && !error && data && status !== 'NOT_FOUND' && (
        <>
          {/* Status summary */}
          <div className={`
            rounded-3xl p-6 border flex items-center gap-4
            ${status === 'VERIFIED' ? 'bg-emerald-500/10 border-emerald-500/20' :
              status === 'REJECTED' ? 'bg-red-500/10 border-red-500/20' :
              'bg-amber-500/10 border-amber-500/20'}
          `}>
            <div className={`w-12 h-12 rounded-2xl flex items-center justify-center
              ${status === 'VERIFIED' ? 'bg-emerald-500/20' : status === 'REJECTED' ? 'bg-red-500/20' : 'bg-amber-500/20'}
            `}>
              {status === 'VERIFIED' ? <ShieldCheck size={22} className="text-emerald-400" /> :
               status === 'REJECTED' ? <ShieldX size={22} className="text-red-400" /> :
               <Clock size={22} className="text-amber-400" />}
            </div>
            <div>
              <p className={`font-bold font-['Geist'] text-sm
                ${status === 'VERIFIED' ? 'text-emerald-300' : status === 'REJECTED' ? 'text-red-300' : 'text-amber-300'}
              `}>
                {status === 'VERIFIED' ? '✓ Xác minh thành công' : status === 'REJECTED' ? '✗ Bị từ chối' : '⏳ Đang xử lý'}
              </p>
              {data.verifiedAt && (
                <p className="text-slate-500 text-xs font-['Geist'] mt-0.5">
                  {new Date(data.verifiedAt).toLocaleString('vi-VN')}
                </p>
              )}
            </div>
          </div>

          {/* Timeline */}
          <div className="bg-slate-800/60 border border-white/10 rounded-3xl p-6">
            <h3 className="text-white font-semibold font-['Geist'] text-sm mb-5">Tiến trình xử lý</h3>
            <StatusTimeline status={status} />
          </div>

          {/* Rejection reason */}
          {status === 'REJECTED' && data.rejectionReason && (
            <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-4 flex items-start gap-2.5">
              <Info size={15} className="text-red-400 flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-red-300 text-sm font-semibold font-['Geist'] mb-1">Lý do từ chối</p>
                <p className="text-red-400/80 text-sm font-['Geist']">{data.rejectionReason}</p>
              </div>
            </div>
          )}

          {/* Request ID */}
          {data.requestId && (
            <div className="bg-slate-800/40 border border-white/5 rounded-2xl p-4">
              <p className="text-slate-500 text-xs font-['Geist'] uppercase tracking-wider mb-1">Mã yêu cầu</p>
              <p className="text-slate-300 text-sm font-mono">{data.requestId}</p>
            </div>
          )}
        </>
      )}
    </div>
  );
}
