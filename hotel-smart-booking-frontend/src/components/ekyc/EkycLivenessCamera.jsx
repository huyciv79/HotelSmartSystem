import { useCallback, useEffect, useRef, useState } from 'react';
import { Camera, Loader2, RefreshCcw, ScanFace, AlertTriangle } from 'lucide-react';
import { validateLivenessFrame } from '../../services/ekycService';

/* ─── Step definitions ────────────────────────────────────────────────────── */
// arrowDir: direction the SVG arrow points  (up/down/left/right/none)
// rotateDeg: CSS rotation for the arrow icon in degrees
const LIVENESS_STEPS = [
  { key: 'center', instruction: 'Nhìn thẳng vào camera', arrowDir: 'none',  maxWidth: 480 },
  { key: 'left',   instruction: 'Quay đầu sang trái',    arrowDir: 'left',  maxWidth: 400 },
  { key: 'right',  instruction: 'Quay đầu sang phải',    arrowDir: 'right', maxWidth: 400 },
  { key: 'up',     instruction: 'Nhìn lên trên',         arrowDir: 'up',    maxWidth: 400 },
  { key: 'down',   instruction: 'Nhìn xuống dưới',       arrowDir: 'down',  maxWidth: 400 },
];

const wait = (ms) => new Promise((resolve) => window.setTimeout(resolve, ms));

/* ─── Directional arrow SVG ──────────────────────────────────────────────── */
// `dir` one of: 'left' | 'right' | 'up' | 'down' | 'none'
function DirectionArrow({ dir, pulse }) {
  if (dir === 'none') return null;

  const rotateMap = { right: 0, down: 90, left: 180, up: 270 };
  const rotate = rotateMap[dir] ?? 0;

  return (
    <div
      className={`pointer-events-none absolute inset-0 flex items-center justify-center ${pulse ? 'opacity-100' : 'opacity-70'}`}
      style={{ transition: 'opacity 0.3s' }}
    >
      {/* Outer div: static rotation only — never mixed with animation */}
      <div style={{ transform: `rotate(${rotate}deg)` }}>
        {/* Inner div: slide animation only — no transform conflict */}
        <div style={{ animation: 'arrowSlide 0.7s ease-in-out infinite alternate' }}>
          <svg width="72" height="72" viewBox="0 0 72 72" fill="none" xmlns="http://www.w3.org/2000/svg">
            {/* Glow background */}
            <circle cx="36" cy="36" r="34" fill="rgba(251,191,36,0.18)" stroke="rgba(251,191,36,0.5)" strokeWidth="2" />
            {/* Arrow body */}
            <path
              d="M20 36 H48"
              stroke="#FCD34D"
              strokeWidth="5"
              strokeLinecap="round"
            />
            {/* Arrowhead */}
            <path
              d="M40 24 L54 36 L40 48"
              stroke="#FCD34D"
              strokeWidth="5"
              strokeLinecap="round"
              strokeLinejoin="round"
              fill="none"
            />
          </svg>
        </div>
      </div>
    </div>
  );
}

/* ─── Capture frame ──────────────────────────────────────────────────────── */
function captureFrame(video, frameName, maxWidth) {
  const canvas = document.createElement('canvas');
  const sourceWidth = video.videoWidth || 1280;
  const sourceHeight = video.videoHeight || 720;
  const scale = Math.min(1, maxWidth / sourceWidth);
  canvas.width = Math.round(sourceWidth * scale);
  canvas.height = Math.round(sourceHeight * scale);

  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('Không thể xử lý ảnh camera.');
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (!blob) { reject(new Error('Không thể lấy frame từ camera.')); return; }
        resolve({
          file: new File([blob], `ekyc-${frameName}-${Date.now()}.jpg`, { type: 'image/jpeg' }),
          previewUrl: URL.createObjectURL(blob),
        });
      },
      'image/jpeg',
      0.9,
    );
  });
}

/* ─── Main component ─────────────────────────────────────────────────────── */
export default function EkycLivenessCamera({ onComplete }) {
  const videoRef   = useRef(null);
  const streamRef  = useRef(null);
  const runRef     = useRef(0);

  const [cameraState,     setCameraState]     = useState('starting');
  const [cameraError,     setCameraError]     = useState('');
  const [isScanning,      setIsScanning]      = useState(false);
  const [stepIndex,       setStepIndex]       = useState(0);
  const [countdown,       setCountdown]       = useState(null);   // 3 → 1 → null
  const [phase,           setPhase]           = useState('idle'); // idle | positioning | holding | validating | completed
  const [completedFrames, setCompletedFrames] = useState({});
  const [flashCapture,    setFlashCapture]    = useState(false);  // white-flash on capture
  const [validationError, setValidationError] = useState('');

  /* ── camera helpers ─────────────────────────────────────────────────────── */
  const stopCamera = useCallback(() => {
    runRef.current += 1;
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
    }
    if (videoRef.current) videoRef.current.srcObject = null;
  }, []);

  const startCamera = useCallback(async () => {
    stopCamera();
    setCameraState('starting');
    setCameraError('');
    setIsScanning(false);
    setStepIndex(0);
    setCountdown(null);
    setPhase('idle');
    setCompletedFrames({});
    setFlashCapture(false);
    setValidationError('');

    try {
      if (!navigator.mediaDevices?.getUserMedia)
        throw new Error('Trình duyệt không hỗ trợ truy cập camera.');

      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user', width: { ideal: 1280 }, height: { ideal: 720 } },
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
          ? 'Camera đang bị chặn. Hãy cấp quyền camera rồi thử lại.'
          : error?.message || 'Không thể mở camera.',
      );
    }
  }, [stopCamera]);

  useEffect(() => {
    const id = window.setTimeout(startCamera, 0);
    return () => { window.clearTimeout(id); stopCamera(); };
  }, [startCamera, stopCamera]);

  /* ── liveness scan ──────────────────────────────────────────────────────── */
  const startLiveness = async () => {
    if (cameraState !== 'ready' || isScanning) return;

    const runId = runRef.current + 1;
    runRef.current = runId;
    const isCurrentRun = () => runRef.current === runId;

    const frames   = {};
    const previews = {};

    setIsScanning(true);
    setCompletedFrames({});
    setValidationError('');

    try {
      let index = 0;
      while (index < LIVENESS_STEPS.length) {
        if (!isCurrentRun()) return;
        const step = LIVENESS_STEPS[index];
        setStepIndex(index);
        setValidationError('');

        /* ── 3-second countdown: show arrow & direction DURING count ── */
        setPhase('positioning');
        for (let value = 3; value >= 1; value -= 1) {
          setCountdown(value);
          await wait(900);                    // ~1 s per tick
          if (!isCurrentRun()) return;
        }
        setCountdown(0);                      // brief "0" flash
        await wait(100);
        if (!isCurrentRun()) return;

        /* ── holding phase: capture immediately ── */
        setCountdown(null);
        setPhase('holding');
        await wait(400);
        if (!isCurrentRun()) return;

        // flash effect
        setFlashCapture(true);
        await wait(120);
        setFlashCapture(false);

        const captured = await captureFrame(videoRef.current, step.key, step.maxWidth);

        /* ── validate frame ── */
        setPhase('validating');
        try {
          const validationResult = await validateLivenessFrame(
            captured.file,
            step.key,
            step.key === 'center' ? null : frames.center,
            step.key === 'right' ? frames.left : null,
          );
          if (!validationResult.passed) {
            URL.revokeObjectURL(captured.previewUrl);
            setValidationError(
              validationResult.reason ||
                `Tư thế ${step.instruction.toLowerCase()} chưa đạt. Vui lòng quét lại.`,
            );
            await wait(2500); // Wait so user can read the error
            continue; // Retry same step
          }
        } catch (err) {
          URL.revokeObjectURL(captured.previewUrl);
          const errMsg =
            err?.response?.data?.message ||
            err?.response?.data?.detail ||
            err.message ||
            'Lỗi kiểm tra khung hình.';
          setValidationError(errMsg);
          await wait(2500);
          continue; // Retry same step
        }

        frames[step.key]   = captured.file;
        previews[step.key] = captured.previewUrl;
        setCompletedFrames({ ...frames });
        setPhase('completed');
        await wait(300);
        if (!isCurrentRun()) return;

        index += 1;
      }

      if (!isCurrentRun()) return;
      setPhase('completed');
      setIsScanning(false);
      onComplete(frames, previews);
    } catch (error) {
      setCameraError(error?.message || 'Không thể hoàn thành liveness.');
      setCameraState('error');
      setIsScanning(false);
      setCountdown(null);
    }
  };

  const currentStep = LIVENESS_STEPS[stepIndex];

  return (
    <div className="flex w-full flex-col items-center gap-4">
      {/* ── keyframe style (injected once) ── */}
      <style>{`
        @keyframes arrowSlide {
          from { transform: translateX(-6px); }
          to   { transform: translateX( 6px); }
        }
        @keyframes countPop {
          0%   { transform: scale(0.6); opacity: 0; }
          50%  { transform: scale(1.15); }
          100% { transform: scale(1);   opacity: 1; }
        }
        @keyframes flashFade {
          0%   { opacity: 0.85; }
          100% { opacity: 0; }
        }
      `}</style>

      <p className="max-w-sm px-4 text-center text-sm text-slate-400 font-['Geist']">
        Camera sẽ tự chụp năm góc khuôn mặt. Hãy{' '}
        <strong className="text-amber-300">xoay đầu ngay khi thấy mũi tên</strong>{' '}
        trong lúc đếm ngược — không chờ đến hết.
      </p>

      {/* ── camera viewport ── */}
      <div className="relative aspect-video w-full max-w-md overflow-hidden rounded-2xl bg-black shadow-2xl">
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
          className="h-full w-full scale-x-[-1] object-cover"
        />

        {/* camera not ready overlay */}
        {cameraState !== 'ready' && (
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-slate-900 p-6 text-center">
            {cameraState === 'starting'
              ? <Loader2 size={32} className="animate-spin text-amber-400" />
              : <Camera size={36} className="text-red-400" />
            }
            <p className="text-xs text-slate-400">{cameraError || 'Đang mở camera...'}</p>
          </div>
        )}

        {/* camera ready overlays */}
        {cameraState === 'ready' && (
          <>
            {/* radial vignette */}
            <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_31%_46%_at_50%_48%,transparent_55%,rgba(0,0,0,0.72)_100%)]" />

            {/* face oval guide */}
            <div className="pointer-events-none absolute left-1/2 top-1/2 h-[82%] w-[48%] -translate-x-1/2 -translate-y-1/2 rounded-[50%] border-2 border-dashed border-emerald-400" />

            {/* ── directional arrow (visible DURING countdown / positioning) ── */}
            {isScanning && phase === 'positioning' && (
              <DirectionArrow dir={currentStep.arrowDir} pulse={countdown <= 1} />
            )}

            {/* step + instruction label */}
            <div className="absolute left-4 top-4 bg-black/70 px-3 py-2 text-[10px] font-bold uppercase tracking-wider text-white">
              {isScanning
                ? `Bước ${stepIndex + 1}/5 · ${currentStep.instruction}`
                : 'Camera sẵn sàng'}
            </div>

            {/* Validation error overlay */}
            {validationError && (
              <div className="absolute inset-x-4 top-16 flex items-start gap-2 rounded-xl border border-red-500/50 bg-red-500/90 p-3 text-white shadow-lg backdrop-blur-md animate-fade-in z-20">
                <AlertTriangle size={18} className="mt-0.5 flex-shrink-0 text-red-200" />
                <p className="text-xs font-semibold leading-relaxed tracking-wide">
                  {validationError}
                </p>
              </div>
            )}

            {/* ── countdown number ── */}
            {countdown !== null && (
              <div className="pointer-events-none absolute inset-0 flex items-end justify-center pb-14">
                <span
                  key={countdown}           /* re-mount triggers animation */
                  className="flex h-16 w-16 items-center justify-center rounded-full border-2 border-amber-400 bg-black/75 text-3xl font-black text-amber-300"
                  style={{ animation: 'countPop 0.25s ease-out forwards' }}
                >
                  {countdown === 0 ? '✓' : countdown}
                </span>
              </div>
            )}

            {/* holding / capture banner */}
            {phase === 'holding' && (
              <div className="absolute bottom-4 left-1/2 -translate-x-1/2 bg-emerald-500/90 px-4 py-2 text-[10px] font-black uppercase tracking-widest text-white">
                Giữ nguyên · Đang chụp
              </div>
            )}

            {/* validating banner */}
            {phase === 'validating' && (
              <div className="absolute bottom-4 left-1/2 -translate-x-1/2 bg-amber-500/90 px-4 py-2 flex items-center gap-2 text-[10px] font-black uppercase tracking-widest text-white">
                <Loader2 size={12} className="animate-spin" />
                Đang kiểm tra
              </div>
            )}

            {/* completed step banner */}
            {phase === 'completed' && (
              <div className="absolute bottom-4 left-1/2 -translate-x-1/2 bg-emerald-700/90 px-4 py-2 text-[10px] font-black uppercase tracking-widest text-emerald-200">
                ✓ Chụp xong bước {stepIndex + 1}
              </div>
            )}

            {/* white flash on capture */}
            {flashCapture && (
              <div
                className="pointer-events-none absolute inset-0 bg-white"
                style={{ animation: 'flashFade 0.15s ease-out forwards' }}
              />
            )}
          </>
        )}
      </div>

      {/* ── progress bar ── */}
      <div className="flex w-full max-w-md gap-1.5">
        {LIVENESS_STEPS.map((step, index) => (
          <span
            key={step.key}
            className={`h-1.5 flex-1 transition-colors ${
              completedFrames[step.key]
                ? 'bg-emerald-400'
                : isScanning && index === stepIndex
                  ? 'animate-pulse bg-amber-400'
                  : 'bg-white/10'
            }`}
          />
        ))}
      </div>

      {/* ── action button ── */}
      {cameraState === 'ready' ? (
        <button
          type="button"
          onClick={startLiveness}
          disabled={isScanning}
          className="flex w-full max-w-xs items-center justify-center gap-2 rounded-full border-none bg-gradient-to-r from-emerald-500 to-green-400 px-8 py-3.5 text-sm font-semibold text-white shadow-lg shadow-emerald-500/20 transition-all hover:scale-[1.02] disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isScanning
            ? <Loader2 size={18} className="animate-spin" />
            : <ScanFace size={18} />
          }
          {isScanning ? currentStep.instruction : 'Bắt đầu kiểm tra liveness'}
        </button>
      ) : (
        <button
          type="button"
          onClick={startCamera}
          className="flex items-center gap-2 rounded-full bg-white/10 px-5 py-3 text-sm text-white"
        >
          <RefreshCcw size={15} />
          Mở lại camera
        </button>
      )}
    </div>
  );
}
