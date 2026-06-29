import { Clock3, Copy, QrCode, RefreshCcw, ShieldCheck } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';

const formatDateTime = (value) => {
  if (!value) return 'Chưa có hạn dùng';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Chưa có hạn dùng';

  return date.toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
};

export default function QrCheckInCard({
  booking,
  qrTokenData,
  isGenerating,
  copied,
  onGenerate,
  onCopy,
}) {
  const qrValue = qrTokenData?.qrPayload || qrTokenData?.token || '';

  return (
    <section className="mt-6 border border-slate-200 bg-slate-50">
      <div className="flex flex-col gap-4 border-b border-slate-200 bg-white px-4 py-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-3">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center bg-primary text-white">
            <QrCode size={20} />
          </span>
          <div>
            <span className="block text-[9px] font-black uppercase tracking-widest text-primary">
              QR Code Check-in
            </span>
            <h4 className="mt-1 text-sm font-black uppercase tracking-wider text-slate-950">
              Mã nhận phòng thông minh
            </h4>
            <p className="mt-1 max-w-xl text-[10px] font-bold uppercase tracking-wider text-slate-500">
              Mã chỉ dùng cho booking {booking?.bookingReference} và sẽ mất hiệu lực sau khi check-in.
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={onGenerate}
          disabled={isGenerating}
          className="inline-flex h-11 items-center justify-center gap-2 bg-slate-950 px-5 text-[10px] font-black uppercase tracking-widest text-white transition-all hover:bg-primary disabled:cursor-not-allowed disabled:bg-slate-300"
        >
          {isGenerating ? (
            <>
              <RefreshCcw size={14} className="animate-spin" />
              <span>Đang tạo mã</span>
            </>
          ) : (
            <>
              <RefreshCcw size={14} />
              <span>{qrValue ? 'Tạo mã mới' : 'Tạo mã QR'}</span>
            </>
          )}
        </button>
      </div>

      <div className="grid grid-cols-1 gap-5 p-4 md:grid-cols-[220px_1fr]">
        <div className="flex min-h-[220px] items-center justify-center bg-white p-4">
          {qrValue ? (
            <QRCodeSVG
              value={qrValue}
              size={180}
              level="M"
              includeMargin
              fgColor="#0f172a"
              bgColor="#ffffff"
            />
          ) : (
            <div className="flex flex-col items-center gap-3 text-center text-slate-400">
              <QrCode size={42} />
              <span className="text-[10px] font-black uppercase tracking-widest">
                Chưa tạo QR
              </span>
            </div>
          )}
        </div>

        <div className="flex flex-col justify-between gap-4">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="border border-slate-200 bg-white p-4">
              <div className="mb-2 flex items-center gap-2 text-primary">
                <ShieldCheck size={15} />
                <span className="text-[9px] font-black uppercase tracking-widest">
                  Điều kiện
                </span>
              </div>
              <p className="m-0 text-xs font-bold leading-relaxed text-slate-600">
                Dùng cho khách đã eKYC, booking QR Code và chưa check-in.
              </p>
            </div>
            <div className="border border-slate-200 bg-white p-4">
              <div className="mb-2 flex items-center gap-2 text-primary">
                <Clock3 size={15} />
                <span className="text-[9px] font-black uppercase tracking-widest">
                  Hạn dùng
                </span>
              </div>
              <p className="m-0 text-xs font-bold leading-relaxed text-slate-600">
                {qrTokenData?.expiresAt ? formatDateTime(qrTokenData.expiresAt) : 'Mã mới có hiệu lực ngắn hạn.'}
              </p>
            </div>
          </div>

          {qrValue && (
            <div className="border border-slate-200 bg-white p-4">
              <span className="mb-2 block text-[9px] font-black uppercase tracking-widest text-slate-400">
                Token QR
              </span>
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                <code className="min-w-0 flex-1 break-all bg-slate-100 px-3 py-2 font-mono text-[11px] font-bold text-slate-700">
                  {qrValue}
                </code>
                <button
                  type="button"
                  onClick={onCopy}
                  className="inline-flex h-10 shrink-0 items-center justify-center gap-2 border border-slate-300 bg-white px-4 text-[10px] font-black uppercase tracking-widest text-slate-700 transition-all hover:border-primary hover:text-primary"
                >
                  <Copy size={13} />
                  <span>{copied ? 'Đã copy' : 'Copy'}</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
