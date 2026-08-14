import { useState, useEffect, useRef } from 'react';
import { getRoomDetail } from '../../services/roomManagementService';
import { 
  X, 
  DoorOpen, 
  Edit, 
  Sparkles, 
  CheckCircle2, 
  AlertCircle, 
  Key, 
  Maximize2, 
  Users, 
  CreditCard,
  Trash2
} from 'lucide-react';

export default function RoomDetailModal({ room, roomType, onClose, onEditRoom, onDeleteRoom, anchorRect }) {
  const [detailData, setDetailData] = useState(null);
  const [positionStyle, setPositionStyle] = useState({ opacity: 0 });
  const popoverRef = useRef(null);

  useEffect(() => {
    const fetchDetail = async () => {
      const roomId = room?.roomId || room?.id;
      if (!roomId) return;
      try {
        const res = await getRoomDetail(roomId);
        if (res && res.success && res.data) {
          setDetailData(res.data);
        } else {
          setDetailData(null);
        }
      } catch (err) {
        console.error('Lỗi khi tải chi tiết phòng:', err);
      }
    };

    fetchDetail();
  }, [room]);

  useEffect(() => {
    const updatePosition = () => {
      const popoverWidth = Math.min(380, window.innerWidth - 32);

      if (!anchorRect) {
        // Fallback: screen center
        setPositionStyle({
          position: 'fixed',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: `${popoverWidth}px`,
          zIndex: 5000,
          opacity: 1
        });
        return;
      }

      const cardCenterX = anchorRect.left + anchorRect.width / 2;
      const cardCenterY = anchorRect.top + anchorRect.height / 2;
      const margin = 16;
      const popoverHeight = popoverRef.current ? popoverRef.current.offsetHeight : 340;

      // Position popover precisely centered over the clicked room card
      let left = cardCenterX - popoverWidth / 2;
      let top = cardCenterY - popoverHeight / 2;

      // Keep safely within screen boundaries
      if (left < margin) left = margin;
      if (left + popoverWidth > window.innerWidth - margin) {
        left = window.innerWidth - popoverWidth - margin;
      }

      if (top < margin) top = margin;
      if (top + popoverHeight > window.innerHeight - margin) {
        top = window.innerHeight - popoverHeight - margin;
      }

      setPositionStyle({
        position: 'fixed',
        left: `${left}px`,
        top: `${top}px`,
        width: `${popoverWidth}px`,
        maxHeight: `calc(100vh - 32px)`,
        zIndex: 5000,
        opacity: 1
      });
    };

    updatePosition();
    const timer = setTimeout(updatePosition, 30);

    window.addEventListener('resize', updatePosition);
    window.addEventListener('scroll', updatePosition, true);

    return () => {
      clearTimeout(timer);
      window.removeEventListener('resize', updatePosition);
      window.removeEventListener('scroll', updatePosition, true);
    };
  }, [anchorRect]);

  if (!room) return null;

  const roomNum = room.roomnumber || room.roomNumber || detailData?.roomNumber || 'N/A';
  const floorNum = room.floornumber !== undefined ? room.floornumber : room.floorNumber || detailData?.floorNumber || 1;
  const status = room.status || detailData?.status || 'Available';
  const adminPasscode = room.adminpasscode || room.adminPasscode || detailData?.adminPasscode || 'N/A';

  // Room type details merging
  const typeObj = roomType || (room.roomType) || {};
  const typeName = room.roomtypename || room.roomTypeName || typeObj.name || detailData?.roomTypeName || 'The Iris Suite';
  const basePrice = typeObj.price || typeObj.basePrice || typeObj.baseprice || 0;
  const adultCapacity = typeObj.adultcapacity || typeObj.adultCapacity || detailData?.adultCapacity || 2;
  const childCapacity = typeObj.childcapacity || typeObj.childCapacity || detailData?.childCapacity || 1;
  const area = typeObj.area || detailData?.area || 35;
  const bedType = typeObj.bedtype || typeObj.bedType || detailData?.bedType || 'King Bed';
  const amenities = typeObj.amenities || 'Wifi tốc độ cao, Smart TV 55", Khóa FaceID/QR, Máy pha cà phê, Điều hòa 2 chiều, Bồn tắm sục, Ban công hướng biển, Hộc an toàn';
  const description = typeObj.description || detailData?.note || 'Phòng nghỉ thiết kế sang trọng theo chuẩn 5 sao với tầm nhìn thoáng đãng và nội thất cao cấp.';

  const getStatusBadge = (st) => {
    switch (st) {
      case 'Available':
        return { label: 'Trống', bg: 'bg-emerald-500/10 text-emerald-700 border-emerald-200/80', dot: 'bg-emerald-500' };
      case 'Occupied':
        return { label: 'Có khách', bg: 'bg-primary/10 text-primary border-primary/20', dot: 'bg-primary' };
      case 'Cleaning':
        return { label: 'Dọn dẹp', bg: 'bg-amber-500/10 text-amber-700 border-amber-200/80', dot: 'bg-amber-500' };
      default:
        return { label: 'Bảo trì', bg: 'bg-slate-500/10 text-slate-700 border-slate-200/80', dot: 'bg-slate-500' };
    }
  };

  const statusBadge = getStatusBadge(status);

  return (
    <>
      {/* Light Backdrop Overlay for closing popover on click outside */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-slate-950/25 backdrop-blur-[2px] z-[4999] transition-opacity duration-200 animate-fade-in"
      />

      {/* Popover Window Centered Directly Over Clicked Room Card */}
      <div
        ref={popoverRef}
        style={positionStyle}
        className="bg-white border border-slate-200/90 rounded-2xl shadow-2xl overflow-hidden flex flex-col font-['Montserrat'] animate-in fade-in zoom-in-95 duration-150 text-slate-900 text-left"
      >
        {/* Modal Header synchronized with system brand (White + Primary Crimson Icon) */}
        <div className="p-3.5 px-4 bg-white border-b border-slate-100 flex justify-between items-center gap-2.5 relative z-10">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-9 h-9 rounded-xl bg-primary text-white flex items-center justify-center font-black text-sm shrink-0 shadow-sm shadow-primary/20">
              <DoorOpen className="w-4.5 h-4.5" />
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-1.5 flex-wrap">
                <h4 className="text-sm font-black uppercase tracking-tight text-slate-900 m-0">
                  Phòng {roomNum}
                </h4>
                <span className="text-[9px] font-extrabold uppercase px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 border border-slate-200/80">
                  Tầng {floorNum}
                </span>
                <span className={`inline-flex items-center gap-1 text-[9px] font-extrabold uppercase px-2 py-0.5 rounded-full border ${statusBadge.bg}`}>
                  <span className={`w-1.5 h-1.5 rounded-full ${statusBadge.dot}`} />
                  {statusBadge.label}
                </span>
              </div>
              <p className="text-[11px] text-slate-500 font-medium m-0 mt-0.5 truncate">
                {typeName}
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="w-7 h-7 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 hover:text-slate-800 transition-colors flex items-center justify-center shrink-0 border-none cursor-pointer"
            title="Đóng"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Modal Content */}
        <div className="p-4 overflow-y-auto space-y-3 flex-1 text-xs max-h-[380px]">
          {/* Quick Specs Grid (2x2) with System Brand Accent */}
          <div className="grid grid-cols-2 gap-2">
            <div className="bg-slate-50/80 border border-slate-100 p-2 rounded-xl flex items-center gap-2">
              <div className="w-7 h-7 rounded-lg bg-primary/10 text-primary flex items-center justify-center shrink-0 font-bold">
                <CreditCard className="w-3.5 h-3.5" />
              </div>
              <div className="min-w-0">
                <span className="block text-[8px] font-bold uppercase text-slate-400">Đơn giá / Đêm</span>
                <span className="text-[11px] font-black text-primary block mt-0.5 truncate">
                  {basePrice ? Number(basePrice).toLocaleString() + ' đ' : 'Liên hệ'}
                </span>
              </div>
            </div>

            <div className="bg-slate-50/80 border border-slate-100 p-2 rounded-xl flex items-center gap-2">
              <div className="w-7 h-7 rounded-lg bg-amber-500/10 text-amber-600 flex items-center justify-center shrink-0">
                <Key className="w-3.5 h-3.5" />
              </div>
              <div className="min-w-0">
                <span className="block text-[8px] font-bold uppercase text-slate-400">Mật khẩu Admin</span>
                <span className="text-[11px] font-mono font-black text-slate-900 block mt-0.5 tracking-wider truncate">
                  {adminPasscode}
                </span>
              </div>
            </div>

            <div className="bg-slate-50/80 border border-slate-100 p-2 rounded-xl flex items-center gap-2">
              <div className="w-7 h-7 rounded-lg bg-blue-500/10 text-blue-600 flex items-center justify-center shrink-0">
                <Users className="w-3.5 h-3.5" />
              </div>
              <div className="min-w-0">
                <span className="block text-[8px] font-bold uppercase text-slate-400">Sức chứa</span>
                <span className="text-[11px] font-bold text-slate-800 block mt-0.5 truncate">
                  {adultCapacity} Lớn{childCapacity > 0 ? `, ${childCapacity} Trẻ` : ''}
                </span>
              </div>
            </div>

            <div className="bg-slate-50/80 border border-slate-100 p-2 rounded-xl flex items-center gap-2">
              <div className="w-7 h-7 rounded-lg bg-purple-500/10 text-purple-600 flex items-center justify-center shrink-0">
                <Maximize2 className="w-3.5 h-3.5" />
              </div>
              <div className="min-w-0">
                <span className="block text-[8px] font-bold uppercase text-slate-400">Diện tích & Giường</span>
                <span className="text-[11px] font-bold text-slate-800 block mt-0.5 truncate" title={`${area} m² • ${bedType}`}>
                  {area} m² • {bedType}
                </span>
              </div>
            </div>
          </div>

          {/* Description Block */}
          <div className="bg-slate-50/60 border border-slate-100 rounded-xl p-2.5 space-y-0.5">
            <div className="flex items-center gap-1.5 text-slate-900 font-extrabold text-[11px]">
              <Sparkles className="w-3 h-3 text-primary" />
              Mô tả hạng phòng
            </div>
            <p className="text-slate-600 text-[10px] leading-relaxed m-0">{description}</p>
          </div>

          {/* Amenities */}
          <div className="space-y-1">
            <span className="block text-[9px] font-bold uppercase tracking-wider text-slate-400">
              Tiện nghi phòng:
            </span>
            <div className="flex flex-wrap gap-1">
              {amenities.split(',').map((item, idx) => (
                <span
                  key={idx}
                  className="px-2 py-0.5 bg-slate-50 border border-slate-200/70 rounded text-slate-700 font-semibold text-[9px] flex items-center gap-1 hover:border-primary/30 transition-colors"
                >
                  <CheckCircle2 className="w-2.5 h-2.5 text-emerald-600 shrink-0" />
                  {item.trim()}
                </span>
              ))}
            </div>
          </div>

          {/* Operating Notes */}
          {(room.note || detailData?.note) && (
            <div className="p-2 bg-amber-50/80 border border-amber-200/60 rounded-xl text-amber-900 flex items-start gap-1.5 text-[10px]">
              <AlertCircle className="w-3.5 h-3.5 text-amber-600 shrink-0 mt-0.5" />
              <div>
                <strong className="block text-[9px] uppercase font-bold">Ghi chú vận hành:</strong>
                <span className="text-slate-700">{room.note || detailData?.note}</span>
              </div>
            </div>
          )}
        </div>

        {/* Modal Footer (Synchronized with System Brand Action Buttons) */}
        <div className="bg-slate-50/80 border-t border-slate-100 p-2.5 px-4 flex justify-between items-center gap-2 shrink-0">
          {onDeleteRoom ? (
            <button
              onClick={() => onDeleteRoom(room)}
              className="px-3 py-1.5 bg-rose-50 hover:bg-rose-600 text-rose-600 hover:text-white font-bold text-xs rounded-xl transition-all cursor-pointer border border-rose-200/80 flex items-center gap-1"
            >
              <Trash2 className="w-3.5 h-3.5" />
              Xóa phòng
            </button>
          ) : <div />}

          <div className="flex items-center gap-2">
            <button
              onClick={onClose}
              className="px-3.5 py-1.5 bg-white border border-slate-200 hover:bg-slate-100 text-slate-700 font-bold text-xs rounded-xl transition-all cursor-pointer"
            >
              Đóng
            </button>

            {onEditRoom && (
              <button
                onClick={() => {
                  onClose();
                  onEditRoom(room);
                }}
                className="px-3.5 py-1.5 bg-primary hover:brightness-110 text-white font-bold text-xs rounded-xl transition-all cursor-pointer border-none shadow-[0_4px_12px_rgba(162,5,19,0.2)] flex items-center gap-1.2"
              >
                <Edit className="w-3.5 h-3.5" />
                Cập nhật
              </button>
            )}
          </div>
        </div>

      </div>
    </>
  );
}
