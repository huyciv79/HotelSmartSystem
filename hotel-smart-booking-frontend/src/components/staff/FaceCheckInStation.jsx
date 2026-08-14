import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Camera,
  CheckCircle2,
  FileText,
  ImageIcon,
  KeyRound,
  Loader2,
  RefreshCcw,
  ScanFace,
  Search,
  ShieldCheck,
  UserRound,
} from 'lucide-react';
import {
  checkFaceReadiness,
  faceCheckInBooking,
  filterBookings,
} from '../../services/bookingService';
import { getEkycDocument } from '../../services/ekycService';

const CHECK_IN_READY_STATUSES = new Set([
  'confirmed',
  'paid',
  'partially paid',
  'partiallypaid',
]);

const isFaceIdBooking = (booking) => {
  const method = String(booking.checkInMethod || '').toLowerCase();
  return method === 'faceid' || method === 'face recognition' || method === 'face id';
};

const isCheckInReadyBooking = (booking) => {
  const normalizedStatus = String(booking.status || '')
    .toLowerCase()
    .replace('-', ' ')
    .trim();
  return CHECK_IN_READY_STATUSES.has(normalizedStatus);
};

const mapBookingForStation = (booking = {}) => ({
  ...booking,
  source: booking.source || 'backend',
  id: booking.id ?? booking.bookingId,
  bookingReference:
    booking.bookingReference ||
    booking.bookingNumber ||
    (booking.bookingId ? `BK-${booking.bookingId}` : ''),
  guestName: booking.guestName || 'Khách hàng The Iris',
  email: booking.email || booking.guestEmail || '',
  roomType: booking.roomType || booking.roomTypeName || '',
  quantity: booking.quantity || 1,
  roomAccesses: booking.roomAccesses || [],
  ekycIdentity: booking.ekycIdentity || null,
  checkInMethod: booking.checkInMethod || booking.checkinmethod || 'Manual',
});

const mergeStationBooking = (current, incoming) => ({
  ...current,
  ...incoming,
  roomAccesses:
    incoming.roomAccesses?.length ? incoming.roomAccesses : current.roomAccesses,
  ekycIdentity: incoming.ekycIdentity || current.ekycIdentity,
});

const getErrorMessage = (error) =>
  error?.response?.data?.message ||
  error?.response?.data?.detail ||
  error?.message ||
  'Không thể xác minh khuôn mặt. Vui lòng thử lại.';

const createExpressLivenessSteps = (direction = 'left') => [
  {
    key: 'center',
    instruction: 'Nhìn thẳng vào camera',
    maxWidth: 480,
    countdown: 1,
    hold: 450,
  },
  {
    key: 'challenge',
    direction,
    instruction:
      direction === 'left'
        ? 'Từ từ quay đầu sang trái'
        : 'Từ từ quay đầu sang phải',
    maxWidth: 400,
    countdown: 2,
    hold: 500,
  },
];

const wait = (milliseconds) =>
  new Promise((resolve) => window.setTimeout(resolve, milliseconds));

const INITIAL_READINESS = {
  ready: false,
  status: 'idle',
  code: 'IDLE',
  reason: 'Đưa một khuôn mặt vào giữa khung để hệ thống kiểm tra.',
};

const formatIdentityValue = (value) => {
  if (value === null || value === undefined) return '—';
  const text = String(value).trim();
  return text || '—';
};

const isVerifiedIdentity = (identity) =>
  String(identity?.status || '').trim().toUpperCase() === 'VERIFIED';

function IdentityInfoRow({ label, value }) {
  return (
    <div className="border-b border-white/5 pb-3">
      <span className="block text-[9px] font-bold uppercase tracking-widest text-white/40">
        {label}
      </span>
      <span className="mt-1.5 block text-xs font-black uppercase tracking-wider text-white break-words">
        {formatIdentityValue(value)}
      </span>
    </div>
  );
}

function IdentityDocumentTile({ label, userId, type }) {
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
      return undefined;
    }

    getEkycDocument(userId, type)
      .then((response) => {
        objectUrl = URL.createObjectURL(response.data);
        setSrc(objectUrl);
      })
      .catch(() => setHasError(true))
      .finally(() => setLoading(false));

    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [userId, type]);

  return (
    <div className="flex flex-col gap-2.5">
      <span className="text-[9px] uppercase tracking-widest text-white/40 font-bold">
        {label}
      </span>
      <div className="relative flex aspect-[4/3] w-full items-center justify-center overflow-hidden border border-white/10 bg-white/[0.03]">
        {loading ? (
          <Loader2 size={18} className="text-slate-600 animate-spin" />
        ) : hasError || !src ? (
          <div className="flex flex-col items-center gap-2 px-3 text-center">
            <ImageIcon size={18} className="text-slate-600" />
            <span className="text-[8px] font-black uppercase tracking-widest text-slate-500">
              Chưa có ảnh
            </span>
          </div>
        ) : (
          <>
            <img
              src={src}
              alt={label}
              onError={() => setHasError(true)}
              onContextMenu={(e) => e.preventDefault()}
              onDragStart={(e) => e.preventDefault()}
              className="h-full w-full object-cover select-none"
            />
            {/* Security Watermark Overlay */}
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

function IdentitySummaryPanel({ identity }) {
  if (!identity) {
    return (
      <div className="border border-amber-900/50 bg-amber-950/20 p-4">
        <div className="flex items-start gap-3">
          <FileText size={18} className="mt-0.5 text-amber-300" />
          <div>
            <span className="block text-[10px] font-black uppercase tracking-widest text-amber-300">
              Chưa có dữ liệu hồ sơ
            </span>
            <p className="mt-1 mb-0 text-[10px] font-bold uppercase tracking-wider leading-relaxed text-amber-100/60">
              Booking này chưa trả về hồ sơ danh tính để đối chiếu nhanh tại quầy.
            </p>
          </div>
        </div>
      </div>
    );
  }

  const verified = isVerifiedIdentity(identity);
  const hometown = identity.hometown || identity.provinceName;

  return (
    <div className="space-y-5 border border-neutral-800 bg-neutral-950 p-5">
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div className="flex items-start gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center border border-green-900/50 bg-green-950/30">
            <ShieldCheck size={20} className="text-green-400" />
          </div>
          <div>
            <span className="block text-[10px] font-black uppercase tracking-[0.22em] text-green-400">
              {verified ? 'ĐÃ XÁC MINH DANH TÍNH' : 'HỒ SƠ ĐANG THEO DÕI'}
            </span>
            <span className="mt-1.5 block text-[10px] font-black uppercase tracking-widest text-white">
              {verified
                ? 'ĐÃ KÍCH HOẠT XÁC MINH KHUÔN MẶT'
                : 'CẦN HỒ SƠ XÁC MINH'}
            </span>
            {identity.verifiedAt && (
              <span className="mt-2 block text-[8px] font-bold uppercase tracking-widest text-slate-500">
                Đã xác minh vào {new Date(identity.verifiedAt).toLocaleDateString('vi-VN')}
              </span>
            )}
          </div>
        </div>
        <span className="w-fit border border-green-900/40 bg-green-950/20 px-3 py-1.5 text-[9px] font-black uppercase tracking-widest text-green-300">
          {formatIdentityValue(identity.status)}
        </span>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <IdentityInfoRow label="Họ và tên" value={identity.fullName} />
        <IdentityInfoRow label="Số CCCD/CMND" value={identity.idNumber} />
        <IdentityInfoRow label="Ngày sinh" value={identity.dateOfBirth} />
        <IdentityInfoRow label="Giới tính" value={identity.gender} />
        <IdentityInfoRow label="Quê quán (suy từ CCCD)" value={hometown} />
        <IdentityInfoRow label="Mã tỉnh CCCD" value={identity.provinceCode} />
      </div>

      <div className="space-y-4">
        <div className="flex items-center gap-2 border-b border-white/5 pb-2">
          <FileText size={14} className="text-primary" />
          <h5 className="m-0 text-xs font-black uppercase tracking-widest text-white">
            Tài liệu đã tải lên
          </h5>
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <IdentityDocumentTile label="Mặt trước CCCD" userId={identity.userId} type="front" />
          <IdentityDocumentTile label="Mặt sau CCCD" userId={identity.userId} type="back" />
          <IdentityDocumentTile
            label="Ảnh chân dung (Selfie)"
            userId={identity.userId}
            type="face"
          />
        </div>
      </div>
    </div>
  );
}

export default function FaceCheckInStation({
  bookings,
  showToast,
  onCheckInCompleted,
  initialBookingId = '',
}) {
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const scanRunRef = useRef(0);
  const readinessRequestRef = useRef(null);

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedBookingId, setSelectedBookingId] = useState(initialBookingId);

  useEffect(() => {
    if (initialBookingId) {
      setSelectedBookingId(String(initialBookingId));
    }
  }, [initialBookingId]);

  const [cameraState, setCameraState] = useState('idle');
  const [cameraError, setCameraError] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [isScanning, setIsScanning] = useState(false);
  const [scanCountdown, setScanCountdown] = useState(null);
  const [scanPhase, setScanPhase] = useState('idle');
  const [livenessStep, setLivenessStep] = useState(0);
  const [livenessFrames, setLivenessFrames] = useState({});
  const [scanSteps, setScanSteps] = useState(() =>
    createExpressLivenessSteps(),
  );
  const [readiness, setReadiness] = useState(INITIAL_READINESS);
  const [result, setResult] = useState(null);
  const [verificationError, setVerificationError] = useState('');
  const [stationBookings, setStationBookings] = useState([]);
  const [isLoadingFaceBookings, setIsLoadingFaceBookings] = useState(false);
  const [faceBookingsError, setFaceBookingsError] = useState('');

  const refreshFaceBookings = useCallback(async () => {
    setIsLoadingFaceBookings(true);
    setFaceBookingsError('');
    try {
      const response = await filterBookings({
        page: 0,
        pageSize: 60,
        statuses: ['Confirmed', 'Paid', 'Partially Paid'],
        checkInMethod: 'FaceID',
        sortBy: 'id',
        sortDirection: 'DESC',
      });

      const content = response?.data?.content || [];
      setStationBookings(content.map(mapBookingForStation));
    } catch (error) {
      console.error('Cannot load FaceID bookings:', error);
      setFaceBookingsError('Chưa thể tải danh sách booking. Vui lòng thử làm mới lại sau ít phút.');
    } finally {
      setIsLoadingFaceBookings(false);
    }
  }, []);

  useEffect(() => {
    const timerId = window.setTimeout(() => {
      refreshFaceBookings();
    }, 0);
    return () => window.clearTimeout(timerId);
  }, [refreshFaceBookings]);

  useEffect(() => {
    const handleReload = () => refreshFaceBookings();
    window.addEventListener('reload-bookings', handleReload);
    return () => window.removeEventListener('reload-bookings', handleReload);
  }, [refreshFaceBookings]);

  const mergedBookings = useMemo(() => {
    const byId = new Map();
    const addBooking = (booking) => {
      const normalized = mapBookingForStation(booking);
      if (!normalized.id) return;
      const key = String(normalized.id);
      byId.set(
        key,
        byId.has(key)
          ? mergeStationBooking(byId.get(key), normalized)
          : normalized,
      );
    };

    bookings.forEach(addBooking);
    stationBookings.forEach(addBooking);
    return Array.from(byId.values());
  }, [bookings, stationBookings]);

  const eligibleBookings = useMemo(
    () =>
      mergedBookings.filter(
        (booking) =>
          booking.source === 'backend' &&
          isCheckInReadyBooking(booking) &&
          (isFaceIdBooking(booking) || String(booking.id) === String(initialBookingId)),
      ),
    [mergedBookings, initialBookingId],
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
      mergedBookings.find(
        (booking) => String(booking.id) === String(selectedBookingId),
      ) || null,
    [mergedBookings, selectedBookingId],
  );

  const stopCamera = useCallback(() => {
    scanRunRef.current += 1;
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
    if (videoRef.current) videoRef.current.srcObject = null;
    setIsScanning(false);
    setScanCountdown(null);
    setScanPhase('idle');
    setCameraState('idle');
    setReadiness(INITIAL_READINESS);
  }, []);

  useEffect(() => () => stopCamera(), [stopCamera]);

  const startCamera = useCallback(async () => {
    if (!selectedBooking) {
      showToast('Vui lòng chọn booking trước.', 'warning');
      return;
    }

    stopCamera();
    setCameraState('starting');
    setCameraError('');
    setResult(null);
    setLivenessStep(0);
    setLivenessFrames({});
    setScanCountdown(null);
    setScanPhase('idle');
    setReadiness({
      ...INITIAL_READINESS,
      status: 'checking',
      code: 'CHECKING',
      reason: 'Đang kiểm tra vị trí khuôn mặt...',
    });

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
      setReadiness(INITIAL_READINESS);
      setCameraError(
        error?.name === 'NotAllowedError'
          ? 'Camera đang bị chặn. Hãy cấp quyền camera cho localhost:5173.'
          : getErrorMessage(error),
      );
    }
  }, [selectedBooking, showToast, stopCamera]);

  const captureFrame = useCallback((frameName, maxWidth) => {
    const video = videoRef.current;
    if (!video || video.readyState < HTMLMediaElement.HAVE_CURRENT_DATA) {
      throw new Error('Camera chưa sẵn sàng để quét.');
    }

    const canvas = document.createElement('canvas');
    const sourceWidth = video.videoWidth || 1280;
    const sourceHeight = video.videoHeight || 720;
    const scale = Math.min(1, maxWidth / sourceWidth);
    canvas.width = Math.round(sourceWidth * scale);
    canvas.height = Math.round(sourceHeight * scale);

    const context = canvas.getContext('2d');
    if (!context) throw new Error('Không thể xử lý ảnh camera.');
    context.drawImage(video, 0, 0, canvas.width, canvas.height);

    return new Promise((resolve, reject) => {
      canvas.toBlob(
        (blob) => {
          if (!blob) {
            reject(new Error('Không thể lấy frame từ camera.'));
            return;
          }
          resolve(
            new File([blob], `face-${frameName}-${Date.now()}.jpg`, {
              type: 'image/jpeg',
            }),
          );
        },
        'image/jpeg',
        0.92,
      );
    });
  }, []);

  const checkCurrentReadiness = useCallback(async (forceFresh = false) => {
    if (readinessRequestRef.current) {
      const pendingRequest = readinessRequestRef.current;
      const pendingResult = await pendingRequest;
      if (!forceFresh) return pendingResult;
      if (readinessRequestRef.current === pendingRequest) {
        readinessRequestRef.current = null;
      }
    }

    const readinessRunId = scanRunRef.current;
    const request = (async () => {
      setReadiness((current) =>
        current.ready
          ? current
          : {
              ...current,
              status: 'checking',
              code: 'CHECKING',
              reason: 'Đang kiểm tra vị trí khuôn mặt...',
            },
      );

      try {
        const preview = await captureFrame('readiness', 480);
        const response = await checkFaceReadiness(preview);
        const readinessData = response?.data;
        if (!readinessData) {
          throw new Error('Hệ thống không trả về trạng thái camera.');
        }

        if (scanRunRef.current !== readinessRunId || !streamRef.current) {
          return null;
        }

        const warningCodes = new Set(['FACE_TOO_SMALL', 'NOT_CENTERED']);
        setReadiness({
          ...readinessData,
          status: readinessData.ready
            ? 'ready'
            : warningCodes.has(readinessData.code)
              ? 'warning'
              : 'error',
        });
        return readinessData;
      } catch (error) {
        if (scanRunRef.current === readinessRunId && streamRef.current) {
          setReadiness({
            ready: false,
            status: 'error',
            code: 'SERVICE_ERROR',
            reason: getErrorMessage(error),
          });
        }
        return null;
      }
    })();

    readinessRequestRef.current = request;
    try {
      return await request;
    } finally {
      if (readinessRequestRef.current === request) {
        readinessRequestRef.current = null;
      }
    }
  }, [captureFrame]);

  useEffect(() => {
    if (cameraState !== 'ready' || isScanning || isVerifying || result) {
      return undefined;
    }

    let stopped = false;
    const pollReadiness = async () => {
      if (!stopped) await checkCurrentReadiness();
    };

    pollReadiness();
    const intervalId = window.setInterval(pollReadiness, 2000);
    return () => {
      stopped = true;
      window.clearInterval(intervalId);
    };
  }, [
    cameraState,
    checkCurrentReadiness,
    isScanning,
    isVerifying,
    result,
  ]);

  const submitLivenessFrames = async (frames) => {
    setIsVerifying(true);
    setVerificationError('');
    try {
      const response = await faceCheckInBooking(
        selectedBooking.id,
        frames.center,
        frames.challenge,
        frames.challenge2,
        frames.challenge3,
        frames.challengeDirection,
      );
      const bookingResult = response?.data;
      if (!bookingResult) {
        throw new Error('Backend không trả về dữ liệu check-in.');
      }

      setResult(bookingResult);
      setStationBookings((current) =>
        current.filter(
          (booking) => String(booking.id) !== String(selectedBooking.id),
        ),
      );
      setVerificationError('');
      onCheckInCompleted?.(selectedBooking.id, bookingResult);
      showToast(
        `Xác minh khuôn mặt và check-in thành công cho ${selectedBooking.guestName}.`,
        'success',
        6000,
      );
      stopCamera();
    } catch (error) {
      const message = getErrorMessage(error);
      setVerificationError(message);
      showToast(message, 'error', 7000);
      setLivenessStep(0);
      setLivenessFrames({});
      setScanPhase('idle');
    } finally {
      setIsVerifying(false);
    }
  };

  const startLivenessScan = async () => {
    if (
      !selectedBooking ||
      cameraState !== 'ready' ||
      isVerifying ||
      isScanning
    ) return;

    setIsScanning(true);
    setScanPhase('readiness');
    const readinessData = await checkCurrentReadiness(true);
    if (!readinessData?.ready) {
      const reason =
        readinessData?.reason ||
        'Camera chưa sẵn sàng. Hãy bảo đảm chỉ có một người nhìn thẳng vào camera.';
      showToast(reason, 'warning', 6000);
      setIsScanning(false);
      setScanPhase('idle');
      return;
    }

    const runId = scanRunRef.current + 1;
    scanRunRef.current = runId;
    const randomByte = new Uint8Array(1);
    window.crypto.getRandomValues(randomByte);
    const challengeDirection = randomByte[0] % 2 === 0 ? 'left' : 'right';
    const activeScanSteps = createExpressLivenessSteps(challengeDirection);
    setScanSteps(activeScanSteps);
    setResult(null);
    setVerificationError('');
    setLivenessStep(0);
    setLivenessFrames({});

    const isCurrentRun = () => scanRunRef.current === runId;
    const frames = {};

    try {
      for (let index = 0; index < activeScanSteps.length; index += 1) {
        if (!isCurrentRun()) return;

        const stepConfig = activeScanSteps[index];
        setLivenessStep(index);
        setScanPhase('positioning');

        for (
          let countdown = stepConfig.countdown;
          countdown >= 1;
          countdown -= 1
        ) {
          setScanCountdown(countdown);
          await wait(750);
          if (!isCurrentRun()) return;
        }

        setScanCountdown(null);
        setScanPhase('reading');
        await wait(stepConfig.hold);
        if (!isCurrentRun()) return;

        if (stepConfig.key === 'challenge') {
          frames.challenge = await captureFrame(
            'challenge-1',
            stepConfig.maxWidth,
          );
          await wait(120);
          if (!isCurrentRun()) return;
          frames.challenge2 = await captureFrame(
            'challenge-2',
            stepConfig.maxWidth,
          );
          await wait(120);
          if (!isCurrentRun()) return;
          frames.challenge3 = await captureFrame(
            'challenge-3',
            stepConfig.maxWidth,
          );
          frames.challengeDirection = challengeDirection;
        } else {
          frames[stepConfig.key] = await captureFrame(
            stepConfig.key,
            stepConfig.maxWidth,
          );
        }
        setLivenessFrames({ ...frames });
        setScanPhase('completed');
        await wait(250);
      }

      if (!isCurrentRun()) return;
      setIsScanning(false);
      setScanPhase('verifying');
      await submitLivenessFrames(frames);
    } catch (error) {
      showToast(getErrorMessage(error), 'error', 7000);
      setLivenessStep(0);
      setLivenessFrames({});
      setScanPhase('idle');
    } finally {
      if (isCurrentRun()) {
        setIsScanning(false);
        setScanCountdown(null);
      }
    }
  };

  const selectBooking = (bookingId) => {
    if (isVerifying || isScanning) return;
    stopCamera();
    setSelectedBookingId(bookingId);
    setResult(null);
    setVerificationError('');
    setCameraError('');
    setLivenessStep(0);
    setLivenessFrames({});
    setScanPhase('idle');
  };

  const currentLivenessStep =
    scanSteps[Math.min(livenessStep, scanSteps.length - 1)];
  const readinessStyle =
    readiness.status === 'ready'
      ? {
          border: 'border-green-400',
          dot: 'bg-green-400',
          text: 'text-green-300',
          label: 'Một khuôn mặt · Sẵn sàng',
        }
      : readiness.status === 'warning' || readiness.status === 'checking'
        ? {
            border: 'border-amber-400',
            dot: 'bg-amber-400',
            text: 'text-amber-300',
            label:
              readiness.status === 'checking'
                ? 'Đang kiểm tra khuôn mặt'
                : 'Cần điều chỉnh vị trí',
          }
        : {
            border: 'border-red-500',
            dot: 'bg-red-500',
            text: 'text-red-300',
            label: 'Camera chưa sẵn sàng',
          };

  return (
    <div className="space-y-6 animate-scale-in">
      <div className="flex flex-col xl:flex-row xl:items-end xl:justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <span className="text-[9px] font-black tracking-[0.22em] text-primary uppercase">
            Manager Lobby Station
          </span>
          <h3 className="text-slate-800 font-black text-xl uppercase tracking-wider mt-2 mb-0">
            Check-in tại sảnh
          </h3>
          <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-2">
            Kiểm tra khách và cấp mã phòng
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-[360px_minmax(0,1fr)] gap-6">
        <section className="bg-white border border-slate-200 min-h-[620px]">
          <div className="p-5 border-b border-slate-100">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h4 className="text-xs font-black uppercase tracking-widest text-slate-800 m-0">
                  Booking chờ xác minh
                </h4>
                <p className="text-[9px] text-slate-500 uppercase tracking-wider mt-1">
                  {isLoadingFaceBookings
                    ? 'Đang tải danh sách...'
                    : `${eligibleBookings.length} booking đủ điều kiện`}
                </p>
              </div>
              <button
                type="button"
                onClick={refreshFaceBookings}
                disabled={isLoadingFaceBookings}
                className="bg-primary/10 text-primary border border-primary/20 px-2.5 py-1 text-[9px] font-black disabled:opacity-50 flex items-center gap-1.5"
              >
                {isLoadingFaceBookings ? (
                  <Loader2 size={12} className="animate-spin" />
                ) : (
                  <RefreshCcw size={12} />
                )}
                LIVE
              </button>
            </div>

            <div className="relative mt-4">
              <Search
                size={15}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
              />
              <input
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                placeholder="Mã booking, tên hoặc email..."
                className="w-full bg-slate-50 border border-slate-200 text-slate-850 text-xs py-3 pl-10 pr-3 outline-none focus:border-primary placeholder:text-slate-400"
              />
            </div>
          </div>

          <div className="max-h-[515px] overflow-y-auto divide-y divide-slate-150">
            {faceBookingsError ? (
              <div className="p-8 text-center">
                <div className="mx-auto flex h-12 w-12 items-center justify-center border border-red-200 bg-red-50 text-red-600">
                  <RefreshCcw size={20} />
                </div>
                <h5 className="mt-4 mb-0 text-xs font-black uppercase tracking-widest text-slate-800">
                  Danh sách chưa sẵn sàng
                </h5>
                <p className="mx-auto mt-2 max-w-[240px] text-[11px] font-semibold leading-relaxed text-slate-500">
                  {faceBookingsError}
                </p>
                <button
                  type="button"
                  onClick={refreshFaceBookings}
                  disabled={isLoadingFaceBookings}
                  className="mt-5 inline-flex items-center justify-center gap-2 border border-primary/20 bg-primary/10 px-3 py-2 text-[9px] font-black uppercase tracking-widest text-primary disabled:opacity-50"
                >
                  <RefreshCcw size={12} className={isLoadingFaceBookings ? 'animate-spin' : ''} />
                  Thử lại
                </button>
              </div>
            ) : isLoadingFaceBookings && filteredBookings.length === 0 ? (
              <div className="p-10 text-center">
                <Loader2 size={32} className="mx-auto text-primary animate-spin" />
                <p className="text-[10px] text-slate-500 uppercase tracking-widest font-bold mt-4 leading-relaxed">
                  Đang tải booking...
                </p>
              </div>
            ) : filteredBookings.length === 0 ? (
              <div className="p-10 text-center">
                <UserRound size={32} className="mx-auto text-slate-400" />
                <p className="text-[10px] text-slate-500 uppercase tracking-widest font-bold mt-4 leading-relaxed">
                  Không có booking đã xác nhận phù hợp
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
                        : 'bg-transparent hover:bg-slate-50 border-y-0 border-r-0 border-l-4 border-l-transparent'
                    }`}
                  >
                    <div className="flex justify-between items-start gap-3">
                      <div className="min-w-0">
                        <span className="block text-[9px] text-primary font-black tracking-widest uppercase">
                          {booking.bookingReference}
                        </span>
                        <span className="block text-xs text-slate-800 font-black uppercase tracking-wide mt-2 truncate">
                          {booking.guestName}
                        </span>
                        <span className="block text-[9px] text-slate-500 mt-1 truncate">
                          {booking.email || 'Không có email'}
                        </span>
                      </div>
                      <ScanFace
                        size={19}
                        className={isSelected ? 'text-primary' : 'text-slate-400'}
                      />
                    </div>
                    <div className="grid grid-cols-2 gap-2 mt-4 pt-3 border-t border-slate-100">
                      <div>
                        <span className="block text-[8px] text-slate-450 uppercase">
                          Ngày nhận
                        </span>
                        <span className="block text-[9px] text-slate-750 font-bold mt-1">
                          {booking.checkInDate}
                        </span>
                      </div>
                      <div>
                        <span className="block text-[8px] text-slate-450 uppercase">
                          Hạng phòng
                        </span>
                        <span className="block text-[9px] text-slate-750 font-bold mt-1 truncate">
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

        <section className="bg-white border border-slate-200 p-5 md:p-7 shadow-sm">
          {!selectedBooking ? (
            <div className="min-h-[560px] flex flex-col items-center justify-center text-center border border-dashed border-slate-200 bg-slate-50/50">
              <div className="w-20 h-20 border border-primary/30 bg-primary/10 flex items-center justify-center">
                <ScanFace size={38} className="text-primary" />
              </div>
              <h4 className="text-slate-800 text-sm font-black uppercase tracking-widest mt-6 mb-0">
                Chọn booking của khách
              </h4>
              <p className="text-[10px] text-slate-500 uppercase tracking-wider max-w-sm mt-3 leading-relaxed">
                Các booking đã xác nhận, bao gồm cả đặt phòng nhóm, sẽ được hiển thị.
              </p>
            </div>
          ) : (
            <div className="space-y-5">
              <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-slate-50 border border-slate-200 p-4">
                <div>
                  <span className="text-[9px] text-primary font-black uppercase tracking-widest">
                    Khách đang check-in
                  </span>
                  <h4 className="text-slate-800 font-black uppercase tracking-wide mt-1 mb-0">
                    {selectedBooking.guestName}
                  </h4>
                  <p className="text-[9px] text-slate-500 mt-1">
                    {selectedBooking.bookingReference} · {selectedBooking.roomType}
                  </p>
                </div>
                <span className="px-3 py-1.5 bg-green-50 border border-green-200 text-green-700 text-[9px] font-black uppercase tracking-widest">
                  Cần xác minh
                </span>
              </div>

              <IdentitySummaryPanel
                identity={selectedBooking.ekycIdentity}
              />

              {result ? (
                <div className="min-h-[475px] flex flex-col items-center justify-center text-center bg-green-50 border border-green-200 p-8">
                  <CheckCircle2 size={56} className="text-green-600" />
                  <span className="text-[10px] text-green-700 font-black uppercase tracking-[0.25em] mt-5">
                    Check-in thành công
                  </span>
                  <h4 className="text-slate-800 text-xl font-black uppercase mt-3 mb-0">
                    {result.roomAccesses?.length > 1
                      ? `${result.roomAccesses.length} phòng đã được cấp`
                      : `Phòng ${result.roomNumber || 'Đang cập nhật'}`}
                  </h4>

                  <div className="w-full max-w-xl grid grid-cols-1 sm:grid-cols-2 gap-3 mt-7">
                    {(result.roomAccesses?.length
                      ? result.roomAccesses
                      : [{
                          roomId: result.roomId,
                          roomNumber: result.roomNumber,
                          roomPassword: result.roomPassword,
                          roomKeyExpiresAt: result.roomKeyExpiresAt,
                        }]
                    ).map((access) => (
                      <div
                        key={access.roomId || access.roomNumber}
                        className="bg-slate-50 border border-slate-200 p-5"
                      >
                        <KeyRound size={20} className="text-primary mx-auto" />
                        <span className="block text-[9px] text-slate-500 uppercase tracking-widest mt-3">
                          Phòng {access.roomNumber || '---'}
                        </span>
                        <strong className="block text-2xl text-slate-800 tracking-[0.22em] mt-2 pl-[0.22em]">
                          {access.roomPassword || '------'}
                        </strong>
                        <span className="block text-[8px] text-slate-655 mt-3">
                          Hiệu lực đến{' '}
                          {access.roomKeyExpiresAt
                            ? new Date(access.roomKeyExpiresAt).toLocaleString('vi-VN')
                            : 'thời điểm check-out'}
                        </span>
                      </div>
                    ))}
                  </div>

                  <button
                    type="button"
                    onClick={() => {
                      setResult(null);
                      setSelectedBookingId('');
                    }}
                    className="mt-6 px-6 py-3 bg-primary text-white border-none cursor-pointer text-[10px] font-black uppercase tracking-widest hover:brightness-110"
                  >
                    Check-in khách tiếp theo
                  </button>
                </div>
              ) : (
                <>
                  {verificationError && (
                    <div
                      role="alert"
                      className="border border-red-200 bg-red-50 px-4 py-3 text-center text-[10px] font-black uppercase tracking-wider text-red-750"
                    >
                      {verificationError}
                    </div>
                  )}
                  <div className="relative aspect-video bg-black border border-slate-200 overflow-hidden">
                    <video
                      ref={videoRef}
                      autoPlay
                      playsInline
                      muted
                      className="w-full h-full object-cover scale-x-[-1]"
                    />

                    {cameraState !== 'ready' && (
                      <div className="absolute inset-0 flex flex-col items-center justify-center bg-slate-900/90 text-center p-6">
                        {cameraState === 'starting' ? (
                          <Loader2 size={34} className="text-primary animate-spin" />
                        ) : (
                          <Camera size={38} className="text-slate-500" />
                        )}
                        <p className="text-[10px] text-slate-400 uppercase tracking-widest font-bold mt-4 max-w-sm leading-relaxed">
                          {cameraError ||
                            'Camera chỉ được bật sau khi Manager xác nhận booking.'}
                        </p>
                      </div>
                    )}

                    {cameraState === 'ready' && (
                      <>
                        <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(ellipse_31%_46%_at_50%_48%,transparent_55%,rgba(0,0,0,0.72)_100%)]" />
                        <div
                          className={`absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[35%] h-[74%] rounded-[50%] border-2 border-dashed pointer-events-none transition-colors ${readinessStyle.border}`}
                        />
                        <div className="absolute top-4 left-4 flex items-center gap-2 bg-black/60 px-3 py-2">
                          <span
                            className={`w-2 h-2 rounded-full ${readinessStyle.dot}`}
                          />
                          <span
                            className={`text-[9px] font-black uppercase tracking-widest ${readinessStyle.text}`}
                          >
                            {isScanning
                              ? scanPhase === 'readiness'
                                ? 'Đang xác nhận khuôn mặt'
                                : 'Đang quét chuyển động'
                              : readinessStyle.label}
                          </span>
                        </div>
                        {isScanning && scanCountdown && (
                          <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                            <span className="w-20 h-20 rounded-full bg-black/65 border-2 border-primary text-white text-4xl font-black flex items-center justify-center">
                              {scanCountdown}
                            </span>
                          </div>
                        )}
                        {isScanning && scanPhase === 'reading' && (
                          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 bg-primary/90 px-4 py-2 text-[10px] text-white font-black uppercase tracking-widest">
                            Giữ nguyên tư thế · Đang chụp
                          </div>
                        )}
                        {!isScanning && (
                          <div
                            className={`absolute bottom-4 left-4 right-4 bg-black/75 border px-4 py-2.5 text-[10px] font-bold text-center ${readinessStyle.border} ${readinessStyle.text}`}
                          >
                            {readiness.reason}
                          </div>
                        )}
                      </>
                    )}
                  </div>

                  <div className="bg-slate-50 border border-slate-200 p-4">
                    <div className="flex items-start gap-3">
                      <div className="w-9 h-9 bg-primary/10 border border-primary/20 flex items-center justify-center shrink-0">
                        <ScanFace size={18} className="text-primary" />
                      </div>
                      <div>
                        <span className="block text-[9px] text-slate-500 uppercase tracking-widest">
                          Hướng dẫn
                        </span>
                        <strong className="block text-sm text-slate-800 mt-1">
                          {isVerifying
                            ? 'Đang đối chiếu với hồ sơ khách hàng'
                            : isScanning
                              ? scanPhase === 'readiness'
                                ? 'Đang xác nhận chỉ có một khuôn mặt'
                                : scanPhase === 'reading'
                                  ? `Giữ nguyên tư thế · ${currentLivenessStep.instruction}`
                                  : currentLivenessStep.instruction
                              : readiness.ready
                                ? 'Sẵn sàng quét chuyển động khuôn mặt'
                                : readiness.reason}
                        </strong>
                        <p className="text-[10px] text-slate-500 mt-1.5 mb-0">
                          {isScanning
                            ? `Bước ${livenessStep + 1}/${scanSteps.length} · Giữ tư thế đến khi hệ thống tự chuyển bước.`
                            : 'Xác minh nhanh chỉ yêu cầu nhìn thẳng và quay theo hướng được hướng dẫn.'}
                        </p>
                        <div className="flex items-center gap-1.5 mt-3">
                          {scanSteps.map((step, index) => {
                            const completed = Boolean(livenessFrames[step.key]);
                            const active = isScanning && index === livenessStep;
                            return (
                              <span
                                key={step.key}
                                className={`h-1.5 flex-1 transition-all ${
                                  completed
                                    ? 'bg-green-500'
                                    : active
                                      ? 'bg-primary animate-pulse'
                                      : 'bg-slate-200'
                                }`}
                              />
                            );
                          })}
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-col sm:flex-row gap-3">
                    {cameraState !== 'ready' ? (
                      <button
                        type="button"
                        onClick={startCamera}
                        disabled={cameraState === 'starting'}
                        className="flex-1 py-4 bg-primary hover:brightness-110 disabled:opacity-50 text-white border-none cursor-pointer font-black text-[10px] uppercase tracking-widest flex items-center justify-center gap-2 rounded-sm shadow-sm transition-all"
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
                          onClick={startLivenessScan}
                          disabled={isVerifying || isScanning || !readiness.ready}
                          className="flex-1 py-4 bg-primary hover:brightness-110 disabled:opacity-50 text-white border-none cursor-pointer font-black text-[10px] uppercase tracking-widest flex items-center justify-center gap-2 rounded-sm shadow-sm transition-all"
                        >
                          {isVerifying || isScanning ? (
                            <Loader2 size={16} className="animate-spin" />
                          ) : (
                            <ScanFace size={17} />
                          )}
                          {isVerifying
                            ? 'Đang xác minh...'
                            : isScanning
                              ? currentLivenessStep.instruction
                              : readiness.ready
                                ? 'Bắt đầu xác minh'
                                : 'Chờ camera sẵn sàng'}
                        </button>
                        <button
                          type="button"
                          onClick={startCamera}
                          disabled={isVerifying || isScanning}
                          className="px-5 py-4 bg-slate-50 border border-slate-200 text-slate-850 cursor-pointer disabled:opacity-50 hover:bg-slate-100 flex items-center justify-center gap-2 text-[10px] font-black uppercase tracking-widest rounded-sm shadow-sm transition-all"
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
