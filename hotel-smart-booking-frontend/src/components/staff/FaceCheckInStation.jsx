import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Camera,
  CheckCircle2,
  KeyRound,
  Loader2,
  RefreshCcw,
  ScanFace,
  Search,
  ShieldCheck,
  UserRound,
} from 'lucide-react';
import { faceCheckInBooking } from '../../services/bookingService';

const isFaceIdBooking = (booking) => {
  const method = String(booking.checkInMethod || '').toLowerCase();
  return method === 'faceid' || method === 'face recognition' || method === 'face id';
};

const getErrorMessage = (error) =>
  error?.response?.data?.message ||
  error?.response?.data?.detail ||
  error?.message ||
  'Không thể xác minh khuôn mặt. Vui lòng thử lại.';

export default function FaceCheckInStation({
  bookings,
  showToast,
  onCheckInCompleted,
}) {
  const videoRef = useRef(null);
  const streamRef = useRef(null);

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedBookingId, setSelectedBookingId] = useState('');
  const [cameraState, setCameraState] = useState('idle');
  const [cameraError, setCameraError] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [result, setResult] = useState(null);

  const eligibleBookings = useMemo(
    () =>
      bookings.filter(
        (booking) =>
          booking.source === 'backend' &&
          booking.status === 'Confirmed' &&
          isFaceIdBooking(booking) &&
          Number(booking.quantity || 1) === 1,
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

  const selectedBooking = useMemo(
    () =>
      bookings.find(
        (booking) => String(booking.id) === String(selectedBookingId),
      ) || null,
    [bookings, selectedBookingId],
  );

  const stopCamera = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
    if (videoRef.current) videoRef.current.srcObject = null;
    setCameraState('idle');
  }, []);

  useEffect(() => () => stopCamera(), [stopCamera]);

  const startCamera = useCallback(async () => {
    if (!selectedBooking) {
      showToast('Vui lòng chọn booking FaceID trước.', 'warning');
      return;
    }

    stopCamera();
    setCameraState('starting');
    setCameraError('');
    setResult(null);

    try {
      if (!navigator.mediaDevices?.getUserMedia) {
        throw new Error('Trình duyệt không hỗ trợ truy cập camera.');
      }

      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: 'user',
          width: { ideal: 1280 },
          height: { ideal: 720 },
        },
        audio: false,
      });

      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      setCameraState('ready');
    } catch (error) {
      setCameraState('error');
      setCameraError(
        error?.name === 'NotAllowedError'
          ? 'Camera đang bị chặn. Hãy cấp quyền camera cho localhost:5173.'
          : getErrorMessage(error),
      );
    }
  }, [selectedBooking, showToast, stopCamera]);

  const captureSelfie = useCallback(() => {
    const video = videoRef.current;
    if (!video || video.readyState < HTMLMediaElement.HAVE_CURRENT_DATA) {
      throw new Error('Camera chưa sẵn sàng để chụp.');
    }

    const canvas = document.createElement('canvas');
    const sourceWidth = video.videoWidth || 1280;
    const sourceHeight = video.videoHeight || 720;
    const scale = Math.min(1, 960 / sourceWidth);
    canvas.width = Math.round(sourceWidth * scale);
    canvas.height = Math.round(sourceHeight * scale);

    const context = canvas.getContext('2d');
    if (!context) throw new Error('Không thể xử lý ảnh camera.');
    context.drawImage(video, 0, 0, canvas.width, canvas.height);

    return new Promise((resolve, reject) => {
      canvas.toBlob(
        (blob) => {
          if (!blob) {
            reject(new Error('Không thể tạo ảnh selfie từ camera.'));
            return;
          }
          resolve(
            new File([blob], `face-check-in-${Date.now()}.jpg`, {
              type: 'image/jpeg',
            }),
          );
        },
        'image/jpeg',
        0.92,
      );
    });
  }, []);

  const verifyFace = async () => {
    if (!selectedBooking || cameraState !== 'ready' || isVerifying) return;

    setIsVerifying(true);
    setResult(null);
    try {
      const selfieImage = await captureSelfie();
      const response = await faceCheckInBooking(selectedBooking.id, selfieImage);
      const bookingResult = response?.data;
      if (!bookingResult) {
        throw new Error('Backend không trả về dữ liệu check-in.');
      }

      setResult(bookingResult);
      onCheckInCompleted?.(selectedBooking.id, bookingResult);
      showToast(
        `Xác minh khuôn mặt và check-in thành công cho ${selectedBooking.guestName}.`,
        'success',
        6000,
      );
      stopCamera();
    } catch (error) {
      showToast(getErrorMessage(error), 'error', 7000);
    } finally {
      setIsVerifying(false);
    }
  };

  const selectBooking = (bookingId) => {
    if (isVerifying) return;
    stopCamera();
    setSelectedBookingId(bookingId);
    setResult(null);
    setCameraError('');
  };

  return (
    <div className="space-y-6 animate-scale-in">
      <div className="flex flex-col xl:flex-row xl:items-end xl:justify-between gap-4 border-b border-neutral-900 pb-5">
        <div>
          <span className="text-[9px] font-black tracking-[0.22em] text-primary uppercase">
            Manager Lobby Station
          </span>
          <h3 className="text-white font-black text-xl uppercase tracking-wider mt-2 mb-0">
            FaceID Check-in tại sảnh
          </h3>
          <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-2">
            Chụp một ảnh selfie, đối chiếu eKYC và cấp mật khẩu phòng
          </p>
        </div>
        <div className="flex items-center gap-3 bg-green-950/30 border border-green-900/40 px-4 py-3">
          <ShieldCheck size={18} className="text-green-400" />
          <div>
            <span className="block text-[9px] font-black uppercase tracking-widest text-green-400">
              Face Matching
            </span>
            <span className="block text-[9px] text-slate-500 mt-0.5">
              Spring Boot → Python Face AI
            </span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-[360px_minmax(0,1fr)] gap-6">
        <section className="bg-[#0f0f12] border border-neutral-900 min-h-[620px]">
          <div className="p-5 border-b border-neutral-900">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h4 className="text-xs font-black uppercase tracking-widest text-white m-0">
                  Booking chờ FaceID
                </h4>
                <p className="text-[9px] text-slate-500 uppercase tracking-wider mt-1">
                  {eligibleBookings.length} booking đủ điều kiện
                </p>
              </div>
              <span className="bg-primary/10 text-primary border border-primary/20 px-2.5 py-1 text-[9px] font-black">
                LIVE
              </span>
            </div>

            <div className="relative mt-4">
              <Search
                size={15}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-600"
              />
              <input
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                placeholder="Mã booking, tên hoặc email..."
                className="w-full bg-neutral-950 border border-neutral-800 text-white text-xs py-3 pl-10 pr-3 outline-none focus:border-primary placeholder:text-slate-700"
              />
            </div>
          </div>

          <div className="max-h-[515px] overflow-y-auto divide-y divide-neutral-900">
            {filteredBookings.length === 0 ? (
              <div className="p-10 text-center">
                <UserRound size={32} className="mx-auto text-slate-700" />
                <p className="text-[10px] text-slate-500 uppercase tracking-widest font-bold mt-4 leading-relaxed">
                  Không có booking FaceID đã xác nhận phù hợp
                </p>
              </div>
            ) : (
              filteredBookings.map((booking) => {
                const isSelected =
                  String(selectedBookingId) === String(booking.id);
                return (
                  <button
                    type="button"
                    key={booking.id}
                    onClick={() => selectBooking(booking.id)}
                    className={`w-full text-left p-5 cursor-pointer transition-all ${
                      isSelected
                        ? 'bg-primary/10 border-y-0 border-r-0 border-l-4 border-l-primary'
                        : 'bg-transparent hover:bg-white/[0.03] border-y-0 border-r-0 border-l-4 border-l-transparent'
                    }`}
                  >
                    <div className="flex justify-between items-start gap-3">
                      <div className="min-w-0">
                        <span className="block text-[9px] text-primary font-black tracking-widest uppercase">
                          {booking.bookingReference}
                        </span>
                        <span className="block text-xs text-white font-black uppercase tracking-wide mt-2 truncate">
                          {booking.guestName}
                        </span>
                        <span className="block text-[9px] text-slate-500 mt-1 truncate">
                          {booking.email || 'Không có email'}
                        </span>
                      </div>
                      <ScanFace
                        size={19}
                        className={isSelected ? 'text-primary' : 'text-slate-700'}
                      />
                    </div>
                    <div className="grid grid-cols-2 gap-2 mt-4 pt-3 border-t border-neutral-900/80">
                      <div>
                        <span className="block text-[8px] text-slate-600 uppercase">
                          Ngày nhận
                        </span>
                        <span className="block text-[9px] text-slate-300 font-bold mt-1">
                          {booking.checkInDate}
                        </span>
                      </div>
                      <div>
                        <span className="block text-[8px] text-slate-600 uppercase">
                          Hạng phòng
                        </span>
                        <span className="block text-[9px] text-slate-300 font-bold mt-1 truncate">
                          {booking.roomType}
                        </span>
                      </div>
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </section>

        <section className="bg-[#0f0f12] border border-neutral-900 p-5 md:p-7">
          {!selectedBooking ? (
            <div className="min-h-[560px] flex flex-col items-center justify-center text-center border border-dashed border-neutral-800 bg-neutral-950/30">
              <div className="w-20 h-20 border border-primary/30 bg-primary/10 flex items-center justify-center">
                <ScanFace size={38} className="text-primary" />
              </div>
              <h4 className="text-white text-sm font-black uppercase tracking-widest mt-6 mb-0">
                Chọn booking của khách
              </h4>
              <p className="text-[10px] text-slate-500 uppercase tracking-wider max-w-sm mt-3 leading-relaxed">
                Chỉ booking Confirmed, phương thức FaceID và một phòng được hiển thị.
              </p>
            </div>
          ) : (
            <div className="space-y-5">
              <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-neutral-950 border border-neutral-900 p-4">
                <div>
                  <span className="text-[9px] text-primary font-black uppercase tracking-widest">
                    Khách đang check-in
                  </span>
                  <h4 className="text-white font-black uppercase tracking-wide mt-1 mb-0">
                    {selectedBooking.guestName}
                  </h4>
                  <p className="text-[9px] text-slate-500 mt-1">
                    {selectedBooking.bookingReference} · {selectedBooking.roomType}
                  </p>
                </div>
                <span className="px-3 py-1.5 bg-green-950/30 border border-green-900/40 text-green-400 text-[9px] font-black uppercase tracking-widest">
                  eKYC required
                </span>
              </div>

              {result ? (
                <div className="min-h-[475px] flex flex-col items-center justify-center text-center bg-green-950/10 border border-green-900/40 p-8">
                  <CheckCircle2 size={56} className="text-green-400" />
                  <span className="text-[10px] text-green-400 font-black uppercase tracking-[0.25em] mt-5">
                    Check-in thành công
                  </span>
                  <h4 className="text-white text-xl font-black uppercase mt-3 mb-0">
                    Phòng {result.roomNumber || 'Đang cập nhật'}
                  </h4>

                  <div className="w-full max-w-sm bg-neutral-950 border border-primary/30 mt-7 p-6">
                    <KeyRound size={22} className="text-primary mx-auto" />
                    <span className="block text-[9px] text-slate-500 uppercase tracking-widest mt-3">
                      Mật khẩu phòng
                    </span>
                    <strong className="block text-4xl text-white tracking-[0.28em] mt-3 pl-[0.28em]">
                      {result.roomPassword || '------'}
                    </strong>
                    <span className="block text-[9px] text-slate-600 mt-4">
                      Hiệu lực đến{' '}
                      {result.roomKeyExpiresAt
                        ? new Date(result.roomKeyExpiresAt).toLocaleString('vi-VN')
                        : 'thời điểm check-out'}
                    </span>
                  </div>

                  <button
                    type="button"
                    onClick={() => {
                      setResult(null);
                      setSelectedBookingId('');
                    }}
                    className="mt-6 px-6 py-3 bg-primary text-white border-none cursor-pointer text-[10px] font-black uppercase tracking-widest"
                  >
                    Check-in khách tiếp theo
                  </button>
                </div>
              ) : (
                <>
                  <div className="relative aspect-video bg-black border border-neutral-800 overflow-hidden">
                    <video
                      ref={videoRef}
                      autoPlay
                      playsInline
                      muted
                      className="w-full h-full object-cover scale-x-[-1]"
                    />

                    {cameraState !== 'ready' && (
                      <div className="absolute inset-0 flex flex-col items-center justify-center bg-[#08080a] text-center p-6">
                        {cameraState === 'starting' ? (
                          <Loader2 size={34} className="text-primary animate-spin" />
                        ) : (
                          <Camera size={38} className="text-slate-700" />
                        )}
                        <p className="text-[10px] text-slate-500 uppercase tracking-widest font-bold mt-4 max-w-sm leading-relaxed">
                          {cameraError ||
                            'Camera chỉ được bật sau khi Manager xác nhận booking.'}
                        </p>
                      </div>
                    )}

                    {cameraState === 'ready' && (
                      <>
                        <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(ellipse_31%_46%_at_50%_48%,transparent_55%,rgba(0,0,0,0.72)_100%)]" />
                        <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[35%] h-[74%] rounded-[50%] border-2 border-dashed border-primary pointer-events-none" />
                        <div className="absolute top-4 left-4 flex items-center gap-2 bg-black/60 px-3 py-2">
                          <span className="w-2 h-2 rounded-full bg-green-400" />
                          <span className="text-[9px] text-white font-black uppercase tracking-widest">
                            Camera sẵn sàng
                          </span>
                        </div>
                      </>
                    )}
                  </div>

                  <div className="bg-neutral-950 border border-neutral-900 p-4">
                    <div className="flex items-start gap-3">
                      <div className="w-9 h-9 bg-primary/10 border border-primary/20 flex items-center justify-center shrink-0">
                        <ScanFace size={18} className="text-primary" />
                      </div>
                      <div>
                        <span className="block text-[9px] text-slate-500 uppercase tracking-widest">
                          Hướng dẫn
                        </span>
                        <strong className="block text-sm text-white mt-1">
                          Nhìn thẳng và giữ khuôn mặt trong khung
                        </strong>
                        <p className="text-[10px] text-slate-500 mt-1.5 mb-0">
                          Giữ camera ổn định, đủ sáng và không che mắt, mũi hoặc miệng.
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-col sm:flex-row gap-3">
                    {cameraState !== 'ready' ? (
                      <button
                        type="button"
                        onClick={startCamera}
                        disabled={cameraState === 'starting'}
                        className="flex-1 py-4 bg-primary hover:brightness-110 disabled:opacity-50 text-white border-none cursor-pointer font-black text-[10px] uppercase tracking-widest flex items-center justify-center gap-2"
                      >
                        {cameraState === 'starting' ? (
                          <Loader2 size={16} className="animate-spin" />
                        ) : (
                          <Camera size={16} />
                        )}
                        Mở camera
                      </button>
                    ) : (
                      <>
                        <button
                          type="button"
                          onClick={verifyFace}
                          disabled={isVerifying}
                          className="flex-1 py-4 bg-primary hover:brightness-110 disabled:opacity-50 text-white border-none cursor-pointer font-black text-[10px] uppercase tracking-widest flex items-center justify-center gap-2"
                        >
                          {isVerifying ? (
                            <Loader2 size={16} className="animate-spin" />
                          ) : (
                            <ScanFace size={17} />
                          )}
                          {isVerifying ? 'Đang đối chiếu...' : 'Chụp và xác minh FaceID'}
                        </button>
                        <button
                          type="button"
                          onClick={startCamera}
                          disabled={isVerifying}
                          className="px-5 py-4 bg-neutral-900 border border-neutral-800 text-slate-300 cursor-pointer disabled:opacity-50 flex items-center justify-center gap-2 text-[10px] font-black uppercase tracking-widest"
                        >
                          <RefreshCcw size={15} />
                          Camera
                        </button>
                      </>
                    )}
                  </div>
                </>
              )}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
