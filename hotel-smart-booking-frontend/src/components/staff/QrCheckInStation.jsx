import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertCircle,
  Camera,
  CheckCircle2,
  ClipboardPaste,
  KeyRound,
  Loader2,
  QrCode,
  RefreshCcw,
  Search,
  ShieldCheck,
  VideoOff,
} from 'lucide-react';
import jsQR from 'jsqr';
import { qrCheckInBooking } from '../../services/bookingService';

const QR_TOKEN_PATTERN = /[A-Za-z0-9_-]{32,}/;

const getErrorMessage = (error) =>
  error?.response?.data?.message ||
  error?.response?.data?.detail ||
  error?.message ||
  'Không thể check-in bằng QR Code. Vui lòng thử lại.';

const isQrBooking = (booking) => {
  const method = String(booking.checkInMethod || '').toLowerCase();
  return method === 'qr code' || method === 'qr' || method === 'qrcode';
};

const isReadyForCheckIn = (booking) => {
  const status = String(booking.status || '').toLowerCase().replace('-', ' ');
  return ['confirmed', 'paid', 'partially paid'].includes(status);
};

const getRoomAccessItems = (bookingResult) => {
  const accesses = Array.isArray(bookingResult?.roomAccesses)
    ? bookingResult.roomAccesses.filter((access) => access?.roomNumber || access?.roomPassword)
    : [];

  if (accesses.length > 0) return accesses;

  if (bookingResult?.roomNumber || bookingResult?.roomPassword) {
    return [{
      roomId: bookingResult.roomId,
      roomNumber: bookingResult.roomNumber,
      floorNumber: null,
      roomPassword: bookingResult.roomPassword,
      roomKeyExpiresAt: bookingResult.roomKeyExpiresAt,
    }];
  }

  return [];
};

const waitForVideoReady = (video) =>
  new Promise((resolve) => {
    if (video.readyState >= HTMLMediaElement.HAVE_METADATA) {
      resolve();
      return;
    }
    video.onloadedmetadata = () => resolve();
  });

const extractQrToken = (rawValue) => {
  const text = String(rawValue || '').trim();
  if (!text) return '';

  try {
    const payload = JSON.parse(text);
    const token = payload.token || payload.qrToken || payload.qrPayload;
    if (token) return String(token).trim();
  } catch {
    // QR payload is usually plain text, so JSON parse failure is expected.
  }

  try {
    const url = new URL(text);
    const token =
      url.searchParams.get('token') ||
      url.searchParams.get('qrToken') ||
      url.searchParams.get('qrPayload');
    if (token) return token.trim();

    const hashToken = url.hash.match(QR_TOKEN_PATTERN)?.[0];
    if (hashToken) return hashToken;
  } catch {
    // Plain token, not a URL.
  }

  return text.match(QR_TOKEN_PATTERN)?.[0] || text;
};

const getCameraStream = async () => {
  const baseVideoConstraints = {
    width: { ideal: 1280 },
    height: { ideal: 720 },
  };

  try {
    return await navigator.mediaDevices.getUserMedia({
      video: {
        ...baseVideoConstraints,
        facingMode: { ideal: 'environment' },
      },
      audio: false,
    });
  } catch (error) {
    if (error?.name === 'NotAllowedError' || error?.name === 'SecurityError') {
      throw error;
    }

    return navigator.mediaDevices.getUserMedia({
      video: baseVideoConstraints,
      audio: false,
    });
  }
};

export default function QrCheckInStation({
  bookings,
  showToast,
  onCheckInCompleted,
}) {
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);
  const scanRunRef = useRef(0);
  const scanLoopRef = useRef(null);
  const isSubmittingRef = useRef(false);
  const lastAttemptRef = useRef({ token: '', at: 0 });
  const showToastRef = useRef(showToast);
  const onCheckInCompletedRef = useRef(onCheckInCompleted);

  const [searchQuery, setSearchQuery] = useState('');
  const [manualToken, setManualToken] = useState('');
  const [cameraState, setCameraState] = useState('idle');
  const [cameraError, setCameraError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [lastScannedToken, setLastScannedToken] = useState('');
  const [result, setResult] = useState(null);

  useEffect(() => {
    showToastRef.current = showToast;
    onCheckInCompletedRef.current = onCheckInCompleted;
  }, [onCheckInCompleted, showToast]);

  const eligibleBookings = useMemo(
    () =>
      bookings.filter(
        (booking) =>
          booking.source === 'backend' &&
          isQrBooking(booking) &&
          isReadyForCheckIn(booking),
      ),
    [bookings],
  );

  const filteredBookings = useMemo(() => {
    const keyword = searchQuery.trim().toLowerCase();
    if (!keyword) return eligibleBookings;
    return eligibleBookings.filter((booking) =>
      [booking.bookingReference, booking.guestName, booking.email]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword)),
    );
  }, [eligibleBookings, searchQuery]);

  const resultAccessItems = useMemo(() => getRoomAccessItems(result), [result]);

  const stopCamera = useCallback(() => {
    scanRunRef.current += 1;
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
    if (videoRef.current) videoRef.current.srcObject = null;
    setCameraState('idle');
  }, []);

  const submitQrToken = useCallback(
    async (rawToken, source = 'manual') => {
      const cleanToken = extractQrToken(rawToken);
      if (!cleanToken) {
        if (source === 'manual') {
          showToastRef.current?.('Vui lòng nhập hoặc quét mã QR check-in.', 'warning');
        }
        return false;
      }

      const now = Date.now();
      const lastAttempt = lastAttemptRef.current;
      if (
        source === 'camera' &&
        lastAttempt.token === cleanToken &&
        now - lastAttempt.at < 2500
      ) {
        return false;
      }

      if (isSubmittingRef.current) return false;

      isSubmittingRef.current = true;
      lastAttemptRef.current = { token: cleanToken, at: now };
      setIsSubmitting(true);
      setCameraError('');
      setLastScannedToken(cleanToken);

      try {
        const response = await qrCheckInBooking(cleanToken);
        const bookingResult = response?.data || {};
        setResult(bookingResult);
        onCheckInCompletedRef.current?.(bookingResult.bookingId, bookingResult);
        setManualToken('');
        showToastRef.current?.(
          `Check-in QR thành công cho đơn ${bookingResult.bookingReference || bookingResult.bookingId || ''}.`,
          'success',
        );
        if (source === 'camera') stopCamera();
        return true;
      } catch (error) {
        console.error('QR check-in failed:', error);
        const message = getErrorMessage(error);
        setCameraError(message);
        showToastRef.current?.(message, 'error');
        return false;
      } finally {
        isSubmittingRef.current = false;
        setIsSubmitting(false);
      }
    },
    [stopCamera],
  );

  const scanLoop = useCallback(
    async (runId) => {
      const video = videoRef.current;

      if (!video || runId !== scanRunRef.current) return;

      try {
        if (
          video.readyState >= HTMLMediaElement.HAVE_CURRENT_DATA &&
          video.videoWidth > 0 &&
          video.videoHeight > 0
        ) {
          if (!canvasRef.current) {
            canvasRef.current = document.createElement('canvas');
          }

          const canvas = canvasRef.current;
          const width = video.videoWidth;
          const height = video.videoHeight;

          if (canvas.width !== width) canvas.width = width;
          if (canvas.height !== height) canvas.height = height;

          const context = canvas.getContext('2d', { willReadFrequently: true });
          if (context) {
            context.drawImage(video, 0, 0, width, height);
            const imageData = context.getImageData(0, 0, width, height);
            const qrCode = jsQR(imageData.data, width, height);

            if (qrCode?.data) {
              const checkedIn = await submitQrToken(qrCode.data, 'camera');
              if (checkedIn) return;
            }
          }
        }
      } catch (error) {
        console.error('QR camera scan error:', error);
        if (error?.name === 'SecurityError') {
          setCameraError('Trình duyệt không cho phép đọc hình ảnh từ camera. Hãy cấp quyền camera rồi thử lại.');
          stopCamera();
          setCameraState('error');
          return;
        }
      }

      if (runId === scanRunRef.current) {
        window.setTimeout(() => scanLoopRef.current?.(runId), 250);
      }
    },
    [stopCamera, submitQrToken],
  );

  useEffect(() => {
    scanLoopRef.current = scanLoop;
  }, [scanLoop]);

  const startCamera = useCallback(async () => {
    stopCamera();
    setCameraState('starting');
    setCameraError('');
    setResult(null);

    try {
      if (!navigator.mediaDevices?.getUserMedia) {
        throw new Error('Trình duyệt không hỗ trợ truy cập camera.');
      }

      const stream = await getCameraStream();

      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await waitForVideoReady(videoRef.current);
        await videoRef.current.play();
      }

      const runId = scanRunRef.current + 1;
      scanRunRef.current = runId;
      setCameraState('scanning');
      scanLoop(runId);
    } catch (error) {
      stopCamera();
      setCameraState('error');
      setCameraError(
        error?.name === 'NotAllowedError'
          ? 'Camera đang bị chặn. Hãy cấp quyền camera cho trình duyệt rồi thử lại.'
          : getErrorMessage(error),
      );
    }
  }, [scanLoop, stopCamera]);

  useEffect(() => {
    const startTimer = window.setTimeout(() => startCamera(), 0);
    return () => {
      window.clearTimeout(startTimer);
      stopCamera();
    };
  }, [startCamera, stopCamera]);

  const handleManualSubmit = (event) => {
    event.preventDefault();
    submitQrToken(manualToken, 'manual');
  };

  const handlePasteToken = async () => {
    try {
      const text = await navigator.clipboard.readText();
      setManualToken(text.trim());
      showToastRef.current?.('Đã dán token QR từ clipboard.', 'success');
    } catch (error) {
      console.error('Clipboard read failed:', error);
      showToastRef.current?.('Không thể đọc clipboard trên trình duyệt này.', 'error');
    }
  };

  return (
    <div className="space-y-6 animate-scale-in text-left">
      <div className="flex flex-col gap-4 border-b border-slate-200 pb-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <span className="inline-flex items-center gap-2 bg-primary px-3 py-1 text-[9px] font-black uppercase tracking-widest text-white">
            <QrCode size={12} />
            QR Code Check-in
          </span>
          <h3 className="mt-4 text-base font-black uppercase tracking-wider text-slate-800">
            Trạm quét QR realtime
          </h3>
          <p className="mt-1 max-w-3xl text-[10px] font-bold uppercase tracking-widest text-slate-500">
            Camera tự bật khi vào tab. Đưa mã QR trước camera laptop, hệ thống sẽ tự xác thực và trả mật khẩu phòng.
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={startCamera}
            disabled={cameraState === 'starting' || cameraState === 'scanning' || isSubmitting}
            className="inline-flex h-10 items-center justify-center gap-2 border border-slate-200 bg-slate-50 px-4 text-[9px] font-black uppercase tracking-widest text-slate-800 transition-all hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:text-slate-400"
          >
            {cameraState === 'starting' ? <Loader2 size={13} className="animate-spin" /> : <RefreshCcw size={13} />}
            <span>Bật lại</span>
          </button>
          <button
            type="button"
            onClick={stopCamera}
            disabled={cameraState !== 'scanning' && cameraState !== 'starting'}
            className="inline-flex h-10 items-center justify-center gap-2 border border-slate-200 bg-slate-50 px-4 text-[9px] font-black uppercase tracking-widest text-slate-800 transition-all hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:text-slate-400"
          >
            <VideoOff size={13} />
            <span>Tắt camera</span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1.25fr)_minmax(320px,0.75fr)]">
        <section className="border border-slate-200 bg-white p-5 shadow-sm">
          <div className="mb-4 flex items-center justify-between gap-3 border-b border-slate-100 pb-3">
            <div>
              <h4 className="m-0 text-xs font-black uppercase tracking-widest text-slate-800">
                Camera laptop
              </h4>
              <p className="mt-1 text-[9px] font-bold uppercase tracking-widest text-slate-500">
                Không cần nhập token, không cần bấm check-in
              </p>
            </div>
            <span className={`px-2.5 py-1 text-[8px] font-black uppercase tracking-widest ${
              cameraState === 'scanning'
                ? 'border border-green-200 bg-green-50 text-green-700'
                : cameraState === 'starting'
                ? 'border border-amber-200 bg-amber-50 text-amber-700'
                : cameraState === 'error'
                ? 'border border-red-200 bg-red-50 text-red-650'
                : 'border border-slate-200 bg-slate-50 text-slate-500'
            }`}>
              {cameraState === 'scanning' ? 'Đang quét realtime' : cameraState === 'starting' ? 'Đang mở' : cameraState === 'error' ? 'Lỗi camera' : 'Đã tắt'}
            </span>
          </div>

          <div className="relative aspect-video overflow-hidden border border-slate-200 bg-black">
            <video
              ref={videoRef}
              playsInline
              muted
              className="h-full w-full object-cover"
            />

            {cameraState === 'starting' && (
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-black/70 text-center">
                <Loader2 size={40} className="animate-spin text-primary" />
                <span className="text-[10px] font-black uppercase tracking-widest text-slate-300">
                  Đang mở camera
                </span>
              </div>
            )}

            {cameraState === 'error' && (
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-black/75 px-6 text-center">
                <AlertCircle size={42} className="text-red-400" />
                <span className="max-w-sm text-[10px] font-black uppercase tracking-widest text-red-100">
                  {cameraError || 'Không thể mở camera'}
                </span>
              </div>
            )}

            {cameraState === 'idle' && (
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-black/70 text-center">
                <Camera size={44} className="text-primary" />
                <span className="max-w-xs text-[10px] font-black uppercase tracking-widest text-slate-400">
                  Camera sẽ tự bật khi vào tab QR Check-in
                </span>
              </div>
            )}

            {cameraState === 'scanning' && (
              <div className="pointer-events-none absolute inset-x-4 bottom-4 flex items-center justify-center">
                <div className="border border-green-500/40 bg-black/65 px-4 py-2 text-[9px] font-black uppercase tracking-widest text-green-300">
                  Đưa mã QR vào vùng camera để tự check-in
                </div>
              </div>
            )}
          </div>

          {cameraError && cameraState !== 'error' && (
            <div className="mt-4 border border-red-200 bg-red-50 p-4 text-xs font-bold uppercase tracking-wider text-red-700">
              {cameraError}
            </div>
          )}

          {lastScannedToken && (
            <div className="mt-4 border border-slate-200 bg-slate-50 p-4">
              <span className="mb-2 block text-[9px] font-black uppercase tracking-widest text-slate-500">
                Token vừa xử lý
              </span>
              <code className="block break-all font-mono text-[11px] font-bold text-slate-650">
                {lastScannedToken}
              </code>
            </div>
          )}
        </section>

        <section className="space-y-5">
          <div className="border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-start gap-3">
              <span className="flex h-10 w-10 items-center justify-center bg-primary text-white">
                {isSubmitting ? <Loader2 size={20} className="animate-spin" /> : <ShieldCheck size={20} />}
              </span>
              <div>
                <h4 className="m-0 text-xs font-black uppercase tracking-widest text-slate-800">
                  Trạng thái quét
                </h4>
                <p className="mt-2 text-[10px] font-bold uppercase tracking-wider text-slate-500">
                  {isSubmitting
                    ? 'Đang xác thực QR với server'
                    : cameraState === 'scanning'
                    ? 'Sẵn sàng nhận mã QR'
                    : 'Camera chưa ở trạng thái quét'}
                </p>
              </div>
            </div>
          </div>

          {result && (
            <div className="border border-green-200 bg-green-50 p-5">
              <div className="mb-4 flex items-center gap-3">
                <CheckCircle2 size={22} className="text-green-600" />
                <div>
                  <h4 className="m-0 text-xs font-black uppercase tracking-widest text-green-700">
                    Check-in thành công
                  </h4>
                  <p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-green-600">
                    {result.bookingReference || `Booking #${result.bookingId}`}
                  </p>
                </div>
              </div>
              <h5 className="mb-3 mt-0 text-[10px] font-black uppercase tracking-widest text-slate-800">
                {resultAccessItems.length > 1
                  ? `${resultAccessItems.length} phòng đã được cấp`
                  : 'Phòng đã được cấp'}
              </h5>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                {resultAccessItems.map((access) => (
                  <div
                    key={access.roomId || access.roomNumber}
                    className="border border-green-150 bg-green-50/50 p-4"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <span className="block text-[8px] font-black uppercase tracking-widest text-green-700">
                          Phòng
                        </span>
                        <span className="mt-1 block text-base font-black uppercase tracking-wider text-slate-800">
                          {access.roomNumber || 'Đang cập nhật'}
                        </span>
                        {access.floorNumber !== null && access.floorNumber !== undefined && (
                          <span className="mt-1 block text-[8px] font-bold uppercase tracking-widest text-green-600">
                            Tầng {access.floorNumber}
                          </span>
                        )}
                      </div>
                      <KeyRound size={18} className="mt-1 shrink-0 text-primary" />
                    </div>
                    <span className="mt-3 block text-[8px] font-black uppercase tracking-widest text-green-700">
                      Mật khẩu
                    </span>
                    <strong className="mt-1 block break-all font-mono text-xl font-black tracking-[0.2em] text-primary">
                      {access.roomPassword || 'Đã khóa'}
                    </strong>
                    <span className="mt-2 block text-[8px] font-bold uppercase tracking-wider text-green-600">
                      Hiệu lực đến{' '}
                      {access.roomKeyExpiresAt
                        ? new Date(access.roomKeyExpiresAt).toLocaleString('vi-VN')
                        : 'thời điểm check-out'}
                    </span>
                  </div>
                ))}
                {resultAccessItems.length === 0 && (
                  <div className="border border-green-150 bg-green-50/50 p-4 text-[10px] font-bold uppercase tracking-widest text-green-600">
                    Đã check-in, phòng sẽ hiển thị trong chi tiết booking.
                  </div>
                )}
              </div>
            </div>
          )}

          <details className="border border-slate-200 bg-white p-5 shadow-sm">
            <summary className="cursor-pointer text-[10px] font-black uppercase tracking-widest text-slate-600 transition-colors hover:text-primary">
              Nhập token thủ công khi cần dự phòng
            </summary>
            <form onSubmit={handleManualSubmit} className="mt-4">
              <label className="mb-2 block text-[9px] font-black uppercase tracking-widest text-slate-500">
                Token QR
              </label>
              <textarea
                value={manualToken}
                onChange={(event) => setManualToken(event.target.value)}
                rows={4}
                placeholder="Dán token QR của khách tại đây"
                className="h-28 w-full resize-none border border-slate-200 bg-slate-50 p-3 font-mono text-xs font-bold text-slate-800 outline-none transition-colors placeholder:text-slate-400 focus:border-primary"
              />

              <div className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
                <button
                  type="button"
                  onClick={handlePasteToken}
                  className="inline-flex h-11 items-center justify-center gap-2 border border-slate-200 bg-slate-50 px-4 text-[10px] font-black uppercase tracking-widest text-slate-800 transition-all hover:border-primary hover:text-primary"
                >
                  <ClipboardPaste size={14} />
                  <span>Dán mã</span>
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="inline-flex h-11 items-center justify-center gap-2 bg-primary px-4 text-[10px] font-black uppercase tracking-widest text-white transition-all hover:brightness-110 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400"
                >
                  {isSubmitting ? <Loader2 size={14} className="animate-spin" /> : <ShieldCheck size={14} />}
                  <span>{isSubmitting ? 'Đang xác thực' : 'Check-in QR'}</span>
                </button>
              </div>
            </form>
          </details>
        </section>
      </div>

      <section className="border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-col gap-3 border-b border-slate-100 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h4 className="m-0 text-xs font-black uppercase tracking-widest text-slate-800">
              Booking QR đang chờ check-in
            </h4>
            <p className="mt-1 text-[9px] font-bold uppercase tracking-widest text-slate-500">
              Danh sách tham chiếu, token QR vẫn được xác thực bởi server
            </p>
          </div>
          <div className="relative w-full sm:w-80">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Tìm booking, khách, email"
              className="h-10 w-full border border-slate-200 bg-slate-50 pl-9 pr-3 text-[10px] font-bold uppercase tracking-widest text-slate-800 outline-none transition-colors placeholder:text-slate-400 focus:border-primary"
            />
          </div>
        </div>

        <div className="divide-y divide-slate-100">
          {filteredBookings.length === 0 ? (
            <div className="py-12 text-center text-xs font-bold uppercase tracking-widest text-slate-500">
              Không có booking QR Code đang chờ check-in
            </div>
          ) : (
            filteredBookings.map((booking) => (
              <div
                key={booking.id}
                className="grid grid-cols-1 gap-4 p-5 transition-colors hover:bg-slate-50/50 lg:grid-cols-[1.2fr_0.8fr_0.6fr]"
              >
                <div>
                  <span className="block text-sm font-black uppercase tracking-wider text-slate-800">
                    {booking.guestName}
                  </span>
                  <span className="mt-1 block text-[10px] font-bold uppercase tracking-wider text-slate-500">
                    {booking.bookingReference} | {booking.roomType}
                  </span>
                </div>
                <div className="text-[10px] font-bold uppercase tracking-wider text-slate-650">
                  {booking.checkInDate} đến {booking.checkOutDate}
                  <span className="mt-1 block text-slate-450">{booking.email}</span>
                </div>
                <div className="flex items-center justify-start gap-2 lg:justify-end">
                  <span className="border border-green-200 bg-green-50 px-2.5 py-1 text-[8px] font-black uppercase tracking-widest text-green-700">
                    {booking.status}
                  </span>
                  <KeyRound size={15} className="text-primary" />
                </div>
              </div>
            ))
          )}
        </div>
      </section>
    </div>
  );
}
