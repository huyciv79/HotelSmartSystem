import { useState, useEffect, useCallback } from 'react';
import { changeRoom, getAvailableRooms } from '../../services/roomChangeService';

// ─────────────────────────────────────────────────────────────────────────────
// UTILITY: Format tiền VND
// ─────────────────────────────────────────────────────────────────────────────
const formatVND = (amount) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount ?? 0);

// ─────────────────────────────────────────────────────────────────────────────
// UTILITY: Tính chênh lệch giá giữa 2 loại phòng
//   Trả về số dương (upgrade), số âm (downgrade), hoặc 0 (cùng loại)
// ─────────────────────────────────────────────────────────────────────────────
const calcPriceDiff = (currentBasePrice, newBasePrice) =>
  (newBasePrice ?? 0) - (currentBasePrice ?? 0);

// ─────────────────────────────────────────────────────────────────────────────
// SUB-COMPONENT: Badge hiển thị chênh lệch giá trên mỗi card phòng
// ─────────────────────────────────────────────────────────────────────────────
function PriceDiffBadge({ diff }) {
  if (diff === 0) {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-bold bg-green-100 text-green-700">
        <span className="material-symbols-outlined text-sm">check_circle</span>
        Miễn phí
      </span>
    );
  }
  if (diff > 0) {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-bold bg-amber-100 text-amber-700">
        <span className="material-symbols-outlined text-sm">arrow_upward</span>
        +{formatVND(diff)}/đêm
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-bold bg-blue-100 text-blue-700">
      <span className="material-symbols-outlined text-sm">arrow_downward</span>
      {formatVND(diff)}/đêm
    </span>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// SUB-COMPONENT: Card hiển thị một phòng Available
// Props:
//   room           – object phòng từ API  { id, roomNumber, floorNumber,
//                                           status, roomtypeid: { name, baseprice, description } }
//   isSelected     – boolean, card đang được chọn
//   currentPrice   – baseprice của phòng hiện tại để tính chênh lệch
//   onSelect       – callback khi click chọn
// ─────────────────────────────────────────────────────────────────────────────
function RoomCard({ room, isSelected, currentPrice, onSelect, roomTypes = [] }) {
  const roomTypeIdVal = room.roomtypeid?.id || room.roomtypeid || room.roomTypeId;
  const roomType = roomTypes.find(rt => rt.id === Number(roomTypeIdVal)) || {
    name: room.roomtypename || room.roomTypeName || 'Không rõ loại phòng',
    baseprice: room.baseprice || 0,
    description: room.description || ''
  };
  const diff = calcPriceDiff(currentPrice, roomType.baseprice);

  return (
    <button
      id={`room-card-${room.id}`}
      type="button"
      onClick={() => onSelect(room)}
      className={`
        w-full text-left border-2 p-4 transition-all duration-150 focus:outline-none
        ${isSelected
          ? 'border-primary bg-primary/5 shadow-md'
          : 'border-surface-container-highest bg-white hover:border-primary/40 hover:shadow-sm'
        }
      `}
      aria-pressed={isSelected}
    >
      {/* Header của card */}
      <div className="flex items-start justify-between gap-2 mb-2">
        <div className="flex items-center gap-2">
          {/* Checkbox tròn — hiển thị trạng thái selected */}
          <div
            className={`
              w-5 h-5 border-2 flex items-center justify-center flex-shrink-0 mt-0.5
              ${isSelected ? 'border-primary bg-primary' : 'border-surface-dim'}
            `}
          >
            {isSelected && (
              <span className="material-symbols-outlined text-white text-xs leading-none">check</span>
            )}
          </div>
          <div>
            <p className="font-bold text-sm text-on-surface uppercase tracking-wide">
              Phòng {room.roomNumber ?? room.roomnumber}
            </p>
            <p className="text-xs text-secondary">
              {roomType.name}
              {(room.floorNumber !== undefined ? room.floorNumber : room.floornumber) && ` · Tầng ${room.floorNumber !== undefined ? room.floorNumber : room.floornumber}`}
            </p>
          </div>
        </div>
        <PriceDiffBadge diff={diff} />
      </div>

      {/* Mô tả ngắn loại phòng */}
      {roomType.description && (
        <p className="text-xs text-secondary mt-1 line-clamp-2 pl-7">
          {roomType.description}
        </p>
      )}

      {/* Footer: giá gốc */}
      <div className="mt-3 pl-7 flex items-center gap-1 text-xs text-secondary">
        <span className="material-symbols-outlined text-sm">payments</span>
        <span>Giá gốc: <strong className="text-on-surface">{formatVND(roomType.baseprice)}</strong>/đêm</span>
      </div>
    </button>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// SUB-COMPONENT: Phần tóm tắt & nút xác nhận (Phần 3)
// ─────────────────────────────────────────────────────────────────────────────
function ConfirmSummary({ booking, selectedRoom, isLoading, onConfirm, roomTypes = [] }) {
  const roomTypeIdVal = selectedRoom?.roomtypeid?.id || selectedRoom?.roomtypeid || selectedRoom?.roomTypeId;
  const roomType = roomTypes.find(rt => rt.id === Number(roomTypeIdVal)) || {
    name: selectedRoom?.roomtypename || selectedRoom?.roomTypeName || '—',
    baseprice: selectedRoom?.baseprice || 0
  };
  const currentPrice = booking?.priceatbooking ?? 0;
  const diff = calcPriceDiff(currentPrice, roomType.baseprice);

  // Xác định loại chuyển phòng để hiển thị màu sắc phù hợp
  const changeTypeLabel =
    diff === 0 ? 'Cùng loại phòng' : diff > 0 ? 'Nâng hạng phòng' : 'Hạ hạng phòng';

  const changeTypeColor =
    diff === 0
      ? 'text-green-700 bg-green-50 border-green-200'
      : diff > 0
      ? 'text-amber-700 bg-amber-50 border-amber-200'
      : 'text-blue-700 bg-blue-50 border-blue-200';

  return (
    <div className="bg-surface-container-low border border-surface-container-highest p-4 space-y-3">
      <h3 className="text-xs font-bold uppercase tracking-widest text-secondary">
        Tóm tắt chuyển phòng
      </h3>

      {selectedRoom ? (
        <>
          {/* Thông tin chuyển */}
          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <span className="text-secondary">Phòng hiện tại</span>
              <span className="font-semibold text-on-surface">
                {booking?.roomNumber ?? '—'}
              </span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-secondary">Chuyển sang</span>
              <span className="font-semibold text-primary">
                Phòng {selectedRoom.roomNumber ?? selectedRoom.roomnumber}
              </span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-secondary">Loại phòng mới</span>
              <span className="font-semibold text-on-surface">{roomType.name ?? '—'}</span>
            </div>
          </div>

          <hr className="border-surface-container-highest" />

          {/* Chênh lệch giá */}
          <div className={`border px-3 py-2 flex items-center justify-between ${changeTypeColor}`}>
            <span className="text-xs font-bold">{changeTypeLabel}</span>
            <span className="text-sm font-bold">
              {diff === 0 ? 'Không đổi' : `${diff > 0 ? '+' : ''}${formatVND(diff)}/đêm`}
            </span>
          </div>

          {/* Ghi chú tiền cho nhân viên */}
          {diff !== 0 && (
            <p className="text-xs text-secondary">
              {diff > 0
                ? '⚠️ Nhân viên cần thu thêm phần chênh lệch từ khách.'
                : 'ℹ️ Ghi nhận credit/hoàn tiền chênh lệch cho khách.'}
            </p>
          )}
        </>
      ) : (
        /* Trạng thái chưa chọn phòng */
        <p className="text-sm text-secondary text-center py-2">
          Vui lòng chọn phòng muốn chuyển ở trên.
        </p>
      )}

      {/* Nút xác nhận */}
      <button
        id="btn-confirm-room-change"
        type="button"
        disabled={!selectedRoom || isLoading}
        onClick={onConfirm}
        className={`
          w-full py-3 text-sm font-bold uppercase tracking-widest
          flex items-center justify-center gap-2 transition-all duration-150
          ${!selectedRoom || isLoading
            ? 'bg-surface-container-highest text-secondary cursor-not-allowed'
            : 'bg-primary text-on-primary hover:bg-primary/90 active:scale-[0.99]'
          }
        `}
      >
        {isLoading ? (
          <>
            {/* Spinner khi loading */}
            <svg
              className="animate-spin h-4 w-4"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
            </svg>
            Đang xử lý...
          </>
        ) : (
          <>
            <span className="material-symbols-outlined text-base">swap_horiz</span>
            Xác nhận gửi yêu cầu
          </>
        )}
      </button>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN COMPONENT: RoomChangeModal
//
// Props:
//   isOpen       – boolean, hiển thị/ẩn modal
//   onClose      – callback đóng modal
//   booking      – object booking đang Checked-in từ parent, gồm:
//                  { bookingId, roomId, roomNumber, roomTypeName, priceatbooking, ... }
//   onSuccess    – callback(result) sau khi chuyển phòng thành công,
//                  để parent reload lại dữ liệu booking
//   showToast    – hàm toast từ useToast() của parent
//
// STATE OVERVIEW:
//   availableRooms  – danh sách phòng Available từ API
//   selectedRoom    – phòng mà nhân viên đã click chọn (object hoặc null)
//   reason          – lý do chuyển phòng (string, tuỳ chọn)
//   isLoadingRooms  – đang fetch danh sách phòng
//   isSubmitting    – đang gọi API xác nhận chuyển phòng
//   error           – thông báo lỗi inline (string hoặc null)
// ─────────────────────────────────────────────────────────────────────────────
export default function RoomChangeModal({ isOpen, onClose, booking, onSuccess, showToast, roomTypes = [] }) {
  // ── State ─────────────────────────────────────────────────────────────────

  /** Danh sách phòng Available lấy từ API */
  const [availableRooms, setAvailableRooms] = useState([]);

  /** Phòng nhân viên đã chọn để chuyển sang */
  const [selectedRoom, setSelectedRoom] = useState(null);

  /** Lý do chuyển phòng (tùy chọn) */
  const [reason, setReason] = useState('');

  /** Trạng thái đang tải danh sách phòng */
  const [isLoadingRooms, setIsLoadingRooms] = useState(false);

  /** Trạng thái đang gửi yêu cầu (loading button) */
  const [isSubmitting, setIsSubmitting] = useState(false);

  /** Thông báo lỗi hiển thị inline trong modal */
  const [error, setError] = useState(null);

  // ── Side Effects ───────────────────────────────────────────────────────────

  /**
   * Khi modal mở: reset toàn bộ state và fetch danh sách phòng trống.
   * Khi modal đóng: không làm gì (tránh gọi API thừa).
   */
  useEffect(() => {
    if (!isOpen) return;

    // Reset state mỗi lần mở modal mới
    setSelectedRoom(null);
    setReason('');
    setError(null);
    fetchAvailableRooms();
  }, [isOpen]); // eslint-disable-line react-hooks/exhaustive-deps

  /**
   * Đóng modal khi nhấn Escape trên bàn phím.
   */
  useEffect(() => {
    if (!isOpen) return;
    const handleKey = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [isOpen, onClose]);

  // ── API Calls ──────────────────────────────────────────────────────────────

  /**
   * Lấy danh sách phòng đang trống từ API.
   * Lọc ra phòng hiện tại của khách (không hiển thị phòng khách đang ở).
   */
  const fetchAvailableRooms = useCallback(async () => {
    setIsLoadingRooms(true);
    setError(null);
    try {
      const res = await getAvailableRooms();
      if (res?.success && res?.data) {
        // Lọc bỏ phòng hiện tại của booking
        const filtered = (res.data.content ?? res.data).filter(
          (r) => r.id !== booking?.roomId
        );
        setAvailableRooms(filtered);
      } else {
        setError('Không thể tải danh sách phòng trống. Vui lòng thử lại.');
      }
    } catch {
      setError('Lỗi kết nối. Không thể tải danh sách phòng.');
    } finally {
      setIsLoadingRooms(false);
    }
  }, [booking?.roomId]);

  /**
   * Gửi yêu cầu chuyển phòng lên API.
   */
  const handleSubmit = async () => {
    if (!selectedRoom) return;

    setIsSubmitting(true);
    setError(null);

    try {
      const payload = {
        bookingId: booking.bookingId,
        newRoomId: selectedRoom.id,
        reason: reason.trim() || undefined, // Không gửi trường rỗng
      };

      const res = await changeRoom(payload);

      if (res?.success) {
        showToast(
          `Đã chuyển phòng thành công: ${booking.roomNumber} → Phòng ${selectedRoom.roomNumber ?? selectedRoom.roomnumber}`,
          'success'
        );
        onSuccess?.(res.data); // Gọi callback để parent reload dữ liệu
        onClose();
      } else {
        setError(res?.message ?? 'Chuyển phòng không thành công. Vui lòng thử lại.');
      }
    } catch (err) {
      const msg = err?.response?.data?.message ?? 'Đã xảy ra lỗi. Vui lòng thử lại.';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  // ── Render Guard ──────────────────────────────────────────────────────────
  if (!isOpen) return null;

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    /* Backdrop overlay */
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title-room-change"
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      {/* Semi-transparent backdrop */}
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" />

      {/* Modal container */}
      <div className="relative z-10 w-full max-w-2xl max-h-[90vh] flex flex-col bg-surface shadow-2xl">

        {/* ── HEADER ─────────────────────────────────────────────────────── */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-surface-container-highest bg-white">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-primary flex items-center justify-center flex-shrink-0">
              <span className="material-symbols-outlined text-white text-base">swap_horiz</span>
            </div>
            <div>
              <h2
                id="modal-title-room-change"
                className="font-bold text-base uppercase tracking-wide text-on-surface"
              >
                Yêu cầu chuyển phòng
              </h2>
              <p className="text-xs text-secondary">
                Mã đặt phòng: <strong>{booking?.bookingReference ?? '—'}</strong>
              </p>
            </div>
          </div>
          <button
            id="btn-close-room-change-modal"
            type="button"
            onClick={onClose}
            aria-label="Đóng"
            className="w-8 h-8 flex items-center justify-center text-secondary hover:text-on-surface hover:bg-surface-container-high transition-colors"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        {/* ── SCROLLABLE BODY ──────────────────────────────────────────────── */}
        <div className="flex-1 overflow-y-auto">

          {/* ── PHẦN 1: Thông tin phòng hiện tại & Lý do ─────────────────── */}
          <section className="px-6 py-5 border-b border-surface-container-highest bg-white">
            <h3 className="text-xs font-bold uppercase tracking-widest text-secondary mb-3">
              Phần 1 — Phòng hiện tại
            </h3>

            {/* Info box phòng hiện tại */}
            <div className="flex items-center gap-3 bg-surface-container-low border border-surface-container-highest px-4 py-3 mb-4">
              <div className="w-10 h-10 bg-primary/10 flex items-center justify-center flex-shrink-0">
                <span className="material-symbols-outlined text-primary">bed</span>
              </div>
              <div>
                <p className="text-sm font-bold text-on-surface">
                  Phòng {booking?.roomNumber ?? '—'} — {booking?.roomTypeName ?? '—'}
                </p>
                <p className="text-xs text-secondary">
                  Khách: <strong>{booking?.guestName ?? 'Không xác định'}</strong>
                  {booking?.priceatbooking && (
                    <> · Giá hiện tại: <strong>{formatVND(booking.priceatbooking)}/đêm</strong></>
                  )}
                </p>
              </div>
            </div>

            {/* Ô nhập lý do */}
            <div>
              <label
                htmlFor="input-room-change-reason"
                className="block text-xs font-bold uppercase tracking-wider text-secondary mb-1.5"
              >
                Lý do chuyển phòng
                <span className="ml-1 font-normal normal-case tracking-normal text-secondary/70">(tuỳ chọn)</span>
              </label>
              <textarea
                id="input-room-change-reason"
                rows={2}
                placeholder="VD: Khách phàn nàn về tiếng ồn, yêu cầu phòng view biển..."
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                maxLength={300}
                className="w-full border border-surface-container-highest bg-white px-3 py-2 text-sm text-on-surface placeholder:text-secondary/50 focus:outline-none focus:border-primary resize-none"
              />
              <p className="text-right text-xs text-secondary/60 mt-0.5">{reason.length}/300</p>
            </div>
          </section>

          {/* ── PHẦN 2: Danh sách phòng trống ───────────────────────────── */}
          <section className="px-6 py-5 border-b border-surface-container-highest">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-xs font-bold uppercase tracking-widest text-secondary">
                Phần 2 — Chọn phòng mới
              </h3>
              {/* Nút refresh danh sách */}
              <button
                id="btn-refresh-available-rooms"
                type="button"
                onClick={fetchAvailableRooms}
                disabled={isLoadingRooms}
                className="flex items-center gap-1 text-xs text-secondary hover:text-primary disabled:opacity-40 transition-colors"
              >
                <span className={`material-symbols-outlined text-sm ${isLoadingRooms ? 'animate-spin' : ''}`}>
                  refresh
                </span>
                Làm mới
              </button>
            </div>

            {/* ── Loading skeleton ── */}
            {isLoadingRooms && (
              <div className="space-y-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="border border-surface-container-highest p-4 animate-pulse">
                    <div className="flex items-center gap-3">
                      <div className="w-5 h-5 bg-surface-container-high rounded-sm" />
                      <div className="flex-1 space-y-2">
                        <div className="h-3 bg-surface-container-high w-1/3" />
                        <div className="h-2 bg-surface-container-high w-1/2" />
                      </div>
                      <div className="h-5 w-16 bg-surface-container-high" />
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* ── Danh sách phòng ── */}
            {!isLoadingRooms && availableRooms.length > 0 && (
              <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
                {availableRooms.map((room) => (
                  <RoomCard
                    key={room.id}
                    room={room}
                    isSelected={selectedRoom?.id === room.id}
                    currentPrice={booking?.priceatbooking ?? 0}
                    onSelect={setSelectedRoom}
                    roomTypes={roomTypes}
                  />
                ))}
              </div>
            )}

            {/* ── Không có phòng trống ── */}
            {!isLoadingRooms && availableRooms.length === 0 && !error && (
              <div className="flex flex-col items-center justify-center py-8 text-secondary gap-2">
                <span className="material-symbols-outlined text-4xl opacity-30">bed</span>
                <p className="text-sm font-medium">Hiện không có phòng trống khả dụng</p>
                <p className="text-xs">Vui lòng thử lại sau hoặc kiểm tra trạng thái các phòng.</p>
              </div>
            )}

            {/* ── Thông báo lỗi fetch ── */}
            {!isLoadingRooms && error && !isSubmitting && (
              <div className="flex items-start gap-2 bg-error-container text-on-error-container px-4 py-3 text-sm">
                <span className="material-symbols-outlined text-base mt-0.5 flex-shrink-0">error</span>
                <span>{error}</span>
              </div>
            )}
          </section>

          {/* ── PHẦN 3: Tóm tắt & Xác nhận ─────────────────────────────── */}
          <section className="px-6 py-5">
            <h3 className="text-xs font-bold uppercase tracking-widest text-secondary mb-3">
              Phần 3 — Xác nhận
            </h3>

            {/* Hiển thị lỗi từ API submit */}
            {error && isSubmitting === false && selectedRoom && (
              <div className="flex items-start gap-2 bg-error-container text-on-error-container px-4 py-3 text-sm mb-3">
                <span className="material-symbols-outlined text-base mt-0.5 flex-shrink-0">error</span>
                <span>{error}</span>
              </div>
            )}

            <ConfirmSummary
              booking={booking}
              selectedRoom={selectedRoom}
              isLoading={isSubmitting}
              onConfirm={handleSubmit}
              roomTypes={roomTypes}
            />
          </section>

        </div>{/* end scrollable body */}
      </div>{/* end modal container */}
    </div>
  );
}
