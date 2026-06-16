import { useRef, useCallback, useState } from 'react';
import Webcam from 'react-webcam';
import imageCompression from 'browser-image-compression';
import { Camera, Loader2 } from 'lucide-react';

const COMPRESSION_OPTIONS = {
  maxSizeMB: 0.3,       // < 300 KB
  maxWidthOrHeight: 1280,
  useWebWorker: true,
  fileType: 'image/jpeg',
};

/**
 * Convert a base64 data URL to a File object.
 */
function base64ToFile(base64, filename = 'capture.jpg', mimeType = 'image/jpeg') {
  const byteString = atob(base64.split(',')[1]);
  const ab = new ArrayBuffer(byteString.length);
  const ia = new Uint8Array(ab);
  for (let i = 0; i < byteString.length; i++) ia[i] = byteString.charCodeAt(i);
  return new File([ab], filename, { type: mimeType });
}

/**
 * @param {'id' | 'selfie'} overlayType - Shape of the guide overlay
 * @param {string} hint - Instruction text shown above
 * @param {(file: File, previewUrl: string) => void} onCapture
 * @param {boolean} mirrored - Mirror webcam (useful for selfie)
 */
export default function EkycCamera({ overlayType = 'id', hint, onCapture, mirrored = false }) {
  const webcamRef = useRef(null);
  const [compressing, setCompressing] = useState(false);
  const [cameraError, setCameraError] = useState(false);

  const handleCapture = useCallback(async () => {
    if (!webcamRef.current) return;
    const screenshot = webcamRef.current.getScreenshot();
    if (!screenshot) return;

    setCompressing(true);
    try {
      const rawFile = base64ToFile(screenshot, 'capture.jpg');
      const compressed = await imageCompression(rawFile, COMPRESSION_OPTIONS);
      const previewUrl = URL.createObjectURL(compressed);
      onCapture(compressed, previewUrl);
    } catch (err) {
      console.error('Image compression failed:', err);
      // Fallback: use uncompressed
      const rawFile = base64ToFile(screenshot, 'capture.jpg');
      onCapture(rawFile, screenshot);
    } finally {
      setCompressing(false);
    }
  }, [onCapture]);

  return (
    <div className="flex flex-col items-center gap-4 w-full">
      {/* Hint text */}
      {hint && (
        <p className="text-center text-sm text-slate-400 font-['Geist'] px-4 max-w-sm">
          {hint}
        </p>
      )}

      {/* Camera container with overlay */}
      <div className="relative w-full max-w-md aspect-video bg-black rounded-2xl overflow-hidden shadow-2xl">
        {cameraError ? (
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-slate-900">
            <Camera size={40} className="text-slate-600" />
            <p className="text-slate-500 text-sm text-center px-6">
              Không thể truy cập camera.<br />Vui lòng cấp quyền camera cho trình duyệt.
            </p>
          </div>
        ) : (
          <>
            <Webcam
              ref={webcamRef}
              audio={false}
              screenshotFormat="image/jpeg"
              screenshotQuality={0.9}
              mirrored={mirrored}
              videoConstraints={{ facingMode: mirrored ? 'user' : 'environment', aspectRatio: 16 / 9 }}
              onUserMediaError={() => setCameraError(true)}
              className="w-full h-full object-cover"
            />

            {/* Dark vignette overlay */}
            <div className="absolute inset-0 pointer-events-none"
              style={{ background: 'radial-gradient(ellipse 70% 60% at 50% 50%, transparent 55%, rgba(0,0,0,0.65) 100%)' }}
            />

            {/* Guide frame overlay */}
            {overlayType === 'id' ? (
              // Rectangular dashed yellow frame for ID card
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                <div
                  className="rounded-xl"
                  style={{
                    width: '80%',
                    height: '65%',
                    border: '2.5px dashed #FACC15',
                    boxShadow: '0 0 0 2000px rgba(0,0,0,0.3)',
                    borderRadius: '12px',
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
                  {/* Top-left */}
                  <span className="absolute top-0 left-0 w-5 h-5 border-t-2 border-l-2 border-yellow-400 rounded-tl-sm" />
                  {/* Top-right */}
                  <span className="absolute top-0 right-0 w-5 h-5 border-t-2 border-r-2 border-yellow-400 rounded-tr-sm" />
                  {/* Bottom-left */}
                  <span className="absolute bottom-0 left-0 w-5 h-5 border-b-2 border-l-2 border-yellow-400 rounded-bl-sm" />
                  {/* Bottom-right */}
                  <span className="absolute bottom-0 right-0 w-5 h-5 border-b-2 border-r-2 border-yellow-400 rounded-br-sm" />
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Capture button */}
      <button
        onClick={handleCapture}
        disabled={compressing || cameraError}
        className="
          relative flex items-center justify-center gap-2.5
          px-8 py-3.5 rounded-full font-['Geist'] font-semibold text-sm
          bg-gradient-to-r from-amber-400 to-yellow-300 text-slate-900
          shadow-lg shadow-amber-500/30
          hover:shadow-amber-500/50 hover:scale-105
          disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100
          transition-all duration-200
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
    </div>
  );
}
