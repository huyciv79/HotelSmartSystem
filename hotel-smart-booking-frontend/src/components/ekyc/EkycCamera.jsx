import { useRef, useCallback, useState, useEffect } from 'react';
import { Camera, Loader2, RefreshCcw, AlertTriangle, AlertCircle } from 'lucide-react';
import { useLanguage } from '../../context/LanguageContext';

/**
 * Tính độ mờ của ảnh dựa trên Laplacian variance.
 * Giá trị thấp => ảnh mờ. Ngưỡng thực tế khoảng 80-120.
 * @param {HTMLCanvasElement} canvas
 * @returns {number}
 */
function computeBlurScore(canvas) {
  const ctx = canvas.getContext('2d');
  if (!ctx) return 9999;
  const { width, height } = canvas;
  const imageData = ctx.getImageData(0, 0, width, height);
  const data = imageData.data;

  // Convert to grayscale
  const gray = new Float32Array(width * height);
  for (let i = 0; i < gray.length; i++) {
    const r = data[i * 4];
    const g = data[i * 4 + 1];
    const b = data[i * 4 + 2];
    gray[i] = 0.299 * r + 0.587 * g + 0.114 * b;
  }

  // Apply Laplacian kernel [ 0,1,0 / 1,-4,1 / 0,1,0 ]
  let sum = 0;
  let sumSq = 0;
  let count = 0;
  for (let y = 1; y < height - 1; y++) {
    for (let x = 1; x < width - 1; x++) {
      const idx = y * width + x;
      const lap =
        gray[idx - width] +
        gray[idx + width] +
        gray[idx - 1] +
        gray[idx + 1] -
        4 * gray[idx];
      sum += lap;
      sumSq += lap * lap;
      count++;
    }
  }
  if (count === 0) return 9999;
  const mean = sum / count;
  return sumSq / count - mean * mean; // variance
}

/**
 * Capture video frame to canvas, optionally flipping horizontally (to un-mirror),
 * then compress to JPEG.
 * @param {HTMLVideoElement} videoElement
 * @param {number} maxDim
 * @param {number} quality
 * @param {boolean} isMirrored - if true, flip canvas horizontally before drawing
 * @returns {Promise<{ file: File, previewUrl: string, blurScore: number }>}
 */
function captureAndCompress(videoElement, maxDim = 1280, quality = 0.85, isMirrored = false) {
  return new Promise((resolve, reject) => {
    try {
      const canvas = document.createElement('canvas');
      let width = videoElement.videoWidth || 640;
      let height = videoElement.videoHeight || 480;

      // Scale keeping aspect ratio
      if (width > maxDim || height > maxDim) {
        if (width > height) {
          height = Math.round((height * maxDim) / width);
          width = maxDim;
        } else {
          width = Math.round((width * maxDim) / height);
          height = maxDim;
        }
      }

      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d');
      if (!ctx) throw new Error('Could not get 2d context from canvas');

      if (isMirrored) {
        // Flip horizontally to undo the CSS mirror so text reads correctly
        ctx.translate(width, 0);
        ctx.scale(-1, 1);
      }
      ctx.drawImage(videoElement, 0, 0, width, height);

      // Reset transform before reading pixels for blur
      if (isMirrored) ctx.setTransform(1, 0, 0, 1, 0, 0);

      const blurScore = computeBlurScore(canvas);

      canvas.toBlob(
        (blob) => {
          if (!blob) { reject(new Error('Canvas toBlob failed')); return; }
          const file = new File([blob], 'ekyc_capture.jpg', { type: 'image/jpeg' });
          const previewUrl = URL.createObjectURL(blob);
          resolve({ file, previewUrl, blurScore });
        },
        'image/jpeg',
        quality
      );
    } catch (err) {
      reject(err);
    }
  });
}

// Blur threshold: score below this value means image is too blurry
const BLUR_THRESHOLD = 80;

export default function EkycCamera({ overlayType = 'id', hint, onCapture, mirrored = false }) {
  const { t } = useLanguage();
  const videoRef = useRef(null);
  const fileInputRef = useRef(null);
  const streamRef = useRef(null);
  // Track whether actual stream is from front (user) camera to know if we must un-mirror
  const actuallyMirroredRef = useRef(false);

  const [loadingStream, setLoadingStream] = useState(true);
  const [cameraError, setCameraError] = useState(false);
  const [compressing, setCompressing] = useState(false);
  const [blurWarning, setBlurWarning] = useState(false);
  const [fileError, setFileError] = useState(null);

  // Initialize camera stream with environment→user fallback for laptops
  const startCamera = useCallback(async () => {
    setLoadingStream(true);
    setCameraError(false);
    setBlurWarning(false);

    // Stop existing stream if any
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
    }

    const baseConstraints = {
      width: { ideal: 1920 },
      height: { ideal: 1080 },
    };

    // For ID card: prefer environment (rear) camera; for selfie: use user (front) camera
    const preferredFacing = mirrored ? 'user' : 'environment';
    const fallbackFacing = 'user';

    const tryGetStream = async (facingMode) => {
      return navigator.mediaDevices.getUserMedia({
        video: { ...baseConstraints, facingMode },
        audio: false,
      });
    };

    try {
      let stream;
      let usedFacing = preferredFacing;
      try {
        stream = await tryGetStream(preferredFacing);
      } catch {
        // Laptop has no rear camera — fall back to front camera
        stream = await tryGetStream(fallbackFacing);
        usedFacing = fallbackFacing;
      }

      // Front (user) camera streams are mirrored by the browser
      actuallyMirroredRef.current = usedFacing === 'user';

      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
      setLoadingStream(false);
    } catch (err) {
      console.error('Failed to get user media:', err);
      setCameraError(true);
      setLoadingStream(false);
    }
  }, [mirrored]);

  useEffect(() => {
    startCamera();

    // Cleanup tracks on unmount
    return () => {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((track) => track.stop());
      }
    };
  }, [startCamera]);

  const handleCapture = async () => {
    if (!videoRef.current || cameraError || loadingStream) return;

    setCompressing(true);
    setBlurWarning(false);
    try {
      const { file, previewUrl, blurScore } = await captureAndCompress(
        videoRef.current,
        1280,
        0.88,
        actuallyMirroredRef.current  // un-mirror canvas if front camera
      );

      if (blurScore < BLUR_THRESHOLD) {
        // Image is too blurry — warn user and do NOT advance
        setBlurWarning(true);
        setCompressing(false);
        return;
      }

      onCapture(file, previewUrl);
    } catch (err) {
      console.error('Failed to capture frame:', err);
    } finally {
      setCompressing(false);
    }
  };

  const handleManualUploadClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Reset input value so selecting the same file triggers onChange again
    e.target.value = '';

    const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
    const fileExtension = file.name ? file.name.split('.').pop().toLowerCase() : '';
    const isValidExtension = ['jpg', 'jpeg', 'png', 'webp'].includes(fileExtension);

    if (!validTypes.includes(file.type) && !isValidExtension) {
      setFileError(t('ekyc_toast_invalid_file_format', 'Invalid file format. Only accepts .jpg, .png, .webp'));
      return;
    }

    setFileError(null);
    setCompressing(true);
    try {
      // Create an image element to draw on canvas for compression
      const img = new Image();
      img.src = URL.createObjectURL(file);
      await new Promise((resolve) => (img.onload = resolve));

      const canvas = document.createElement('canvas');
      let width = img.width;
      let height = img.height;
      const maxDim = 1280;

      if (width > maxDim || height > maxDim) {
        if (width > height) {
          height = Math.round((height * maxDim) / width);
          width = maxDim;
        } else {
          width = Math.round((width * maxDim) / height);
          height = maxDim;
        }
      }

      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(img, 0, 0, width, height);

      canvas.toBlob(
        (blob) => {
          if (!blob) {
            const previewUrl = URL.createObjectURL(file);
            onCapture(file, previewUrl);
          } else {
            const compressedFile = new File([blob], file.name, { type: 'image/jpeg' });
            const previewUrl = URL.createObjectURL(blob);
            onCapture(compressedFile, previewUrl);
          }
          setCompressing(false);
        },
        'image/jpeg',
        0.85
      );
    } catch (err) {
      console.error('Manual file processing failed:', err);
      const previewUrl = URL.createObjectURL(file);
      onCapture(file, previewUrl);
      setCompressing(false);
    }
  };

  return (
    <div className="flex flex-col items-center gap-4 w-full">
      {/* Hint text */}
      {hint && (
        <p className="text-center text-sm text-slate-400 font-['Geist'] px-4 max-w-sm">
          {hint}
        </p>
      )}

      {/* File format error banner */}
      {fileError && (
        <div className="flex items-center gap-2 bg-red-50 border border-red-200 rounded-none px-4 py-2.5 w-full max-w-md">
          <AlertCircle size={16} className="text-red-600 flex-shrink-0" />
          <p className="text-red-700 text-xs font-semibold leading-snug">
            {fileError}
          </p>
        </div>
      )}

      {/* Blur warning banner */}
      {blurWarning && (
        <div className="flex items-center gap-2 bg-red-50 border border-red-200 rounded-none px-4 py-2.5 w-full max-w-md">
          <AlertCircle size={16} className="text-red-600 flex-shrink-0" />
          <p className="text-red-700 text-xs font-semibold leading-snug">
            Ảnh bị mờ — vui lòng giữ thẻ <strong>thẳng &amp; bất động</strong>, đảm bảo đủ sáng rồi chụp lại.
          </p>
        </div>
      )}

      {/* Camera container with overlay */}
      <div className="relative w-full max-w-md aspect-video bg-black rounded-none overflow-hidden shadow-lg border border-outline-variant">
        {loadingStream && !cameraError && (
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-slate-900">
            <Loader2 size={32} className="text-primary animate-spin" />
            <p className="text-slate-500 text-xs font-semibold">Đang khởi động camera...</p>
          </div>
        )}

        {cameraError ? (
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-3.5 bg-slate-900 p-6 border border-white/5">
            <Camera size={36} className="text-red-400" />
            <p className="text-red-200 text-xs text-center font-semibold leading-relaxed">
              Camera access is required for automated eKYC. Please grant camera permissions in your browser settings or use the Manual Upload alternative.
            </p>
            <button
              type="button"
              onClick={handleManualUploadClick}
              className="px-4 py-2 bg-primary hover:bg-slate-950 text-white font-semibold uppercase text-[10px] tracking-widest transition-all cursor-pointer rounded-none border-none parallelogram-btn"
            >
              Chọn ảnh từ thiết bị
            </button>
          </div>
        ) : (
          <>
            <video
              ref={videoRef}
              autoPlay
              playsInline
              muted
              // Mirror display when using front camera so user sees a natural preview.
              // The actual capture un-mirrors via canvas transform.
              className={`w-full h-full object-cover ${actuallyMirroredRef.current ? 'scale-x-[-1]' : ''}`}
            />

            {/* Dark vignette overlay */}
            <div className="absolute inset-0 pointer-events-none"
              style={{ background: 'radial-gradient(ellipse 70% 60% at 50% 50%, transparent 55%, rgba(0,0,0,0.65) 100%)' }}
            />

            {/* Guide frame overlay */}
            {overlayType === 'id' ? (
              // Rectangular dashed primary red frame for ID card
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                <div
                  className="rounded-none"
                  style={{
                    width: '80%',
                    height: '65%',
                    border: '2.5px dashed #a20513',
                    boxShadow: '0 0 0 2000px rgba(0,0,0,0.3)',
                    borderRadius: '0px',
                  }}
                />
              </div>
            ) : (
              // Oval dashed green frame for selfie
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                <div
                  style={{
                    width: '55%',
                    height: '88%',
                    border: '2.5px dashed #22C55E',
                    borderRadius: '50%',
                    boxShadow: '0 0 0 2000px rgba(0,0,0,0.3)',
                  }}
                />
              </div>
            )}

            {/* Corner accent decorations for ID frame */}
            {overlayType === 'id' && (
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                <div className="relative" style={{ width: '80%', height: '65%' }}>
                  <span className="absolute top-0 left-0 w-5 h-5 border-t-2 border-l-2 border-primary rounded-none" />
                  <span className="absolute top-0 right-0 w-5 h-5 border-t-2 border-r-2 border-primary rounded-none" />
                  <span className="absolute bottom-0 left-0 w-5 h-5 border-b-2 border-l-2 border-primary rounded-none" />
                  <span className="absolute bottom-0 right-0 w-5 h-5 border-b-2 border-r-2 border-primary rounded-none" />
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Manual upload helper */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
        className="hidden"
      />

      {/* Action buttons */}
      <div className="flex flex-col items-center gap-3 w-full">
        {!cameraError && !loadingStream && (
          <button
            onClick={handleCapture}
            disabled={compressing}
            className="
              relative flex items-center justify-center gap-2.5
              px-8 py-3.5 rounded-none font-semibold text-xs uppercase tracking-widest
              bg-primary hover:bg-slate-950 text-white
              disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100
              transition-all duration-200 w-full max-w-[200px] border-none cursor-pointer
              parallelogram-btn h-12
            "
          >
            {compressing ? (
              <>
                <Loader2 size={18} className="animate-spin" />
                <span>Đang xử lý...</span>
              </>
            ) : (
              <>
                <Camera size={18} />
                <span>Chụp ảnh</span>
              </>
            )}
          </button>
        )}

        {!cameraError && (
          <div className="flex gap-4">
            <button
              type="button"
              onClick={handleManualUploadClick}
              disabled={compressing}
              className="text-xs text-slate-400 hover:text-primary transition-colors uppercase font-semibold tracking-wider underline border-none bg-transparent cursor-pointer"
            >
              Hoặc tải ảnh từ thiết bị
            </button>
            {!loadingStream && (
              <button
                type="button"
                onClick={startCamera}
                className="text-xs text-slate-400 hover:text-primary transition-colors uppercase font-semibold tracking-wider flex items-center gap-1 border-none bg-transparent cursor-pointer"
              >
                <RefreshCcw size={10} /> Khởi động lại camera
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
