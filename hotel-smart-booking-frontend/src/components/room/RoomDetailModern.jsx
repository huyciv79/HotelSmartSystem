import { useState, useEffect } from 'react';
import { Maximize, Users, Check, X, ChevronLeft, ChevronRight } from 'lucide-react';
import ReviewSection from './ReviewSection';
import { useToast } from '../Toast';
import { useLanguage } from '../../context/LanguageContext';

export default function RoomDetailModern({ roomDetailData, onClose, onBookingPersonal, onBookingGroup, onPrevRoomType, onNextRoomType }) {
  const { showToast } = useToast();
  const { t } = useLanguage();
  
  // Extract all images including primary image and secondary ones
  const allImages = [];
  if (roomDetailData?.primaryImageUrl) {
    allImages.push(roomDetailData.primaryImageUrl);
  }
  if (roomDetailData?.images && Array.isArray(roomDetailData.images)) {
    roomDetailData.images.forEach(img => {
      if (img.imageUrl && img.imageUrl !== roomDetailData.primaryImageUrl) {
        allImages.push(img.imageUrl);
      }
    });
  }
  // Fallback image if list is empty
  if (allImages.length === 0) {
    allImages.push('https://images.unsplash.com/photo-1590490360182-c33d57733427?w=1000&q=80');
  }

  const [activeIndex, setActiveIndex] = useState(0);
  const [touchStart, setTouchStart] = useState(null);
  const [touchEnd, setTouchEnd] = useState(null);

  const minSwipeDistance = 40;

  const handleTouchStart = (e) => {
    setTouchEnd(null);
    setTouchStart(e.targetTouches[0].clientX);
  };

  const handleTouchMove = (e) => {
    setTouchEnd(e.targetTouches[0].clientX);
  };

  const handleTouchEnd = () => {
    if (!touchStart || !touchEnd) return;
    const distance = touchStart - touchEnd;
    const isLeftSwipe = distance > minSwipeDistance;
    const isRightSwipe = distance < -minSwipeDistance;

    if (isLeftSwipe) {
      if (activeIndex < allImages.length - 1) {
        setActiveIndex(prev => prev + 1);
      } else if (onNextRoomType) {
        onNextRoomType();
      }
    } else if (isRightSwipe) {
      if (activeIndex > 0) {
        setActiveIndex(prev => prev - 1);
      } else if (onPrevRoomType) {
        onPrevRoomType();
      }
    }
  };

  // Reset active index when roomDetailData changes
  useEffect(() => {
    setTimeout(() => {
      setActiveIndex(0);
    }, 0);
  }, [roomDetailData]);

  if (!roomDetailData) return null;

  return (
    <div className="bg-white rounded-none max-w-5xl w-full my-4 md:my-8 shadow-2xl relative border border-outline-variant animate-scale-in text-slate-800 font-['Montserrat'] overflow-hidden flex flex-col max-h-[90vh]">
      {/* Floating Close Button */}
      <button
        onClick={onClose}
        className="absolute top-4 right-4 z-50 w-9 h-9 rounded-full bg-black/55 hover:bg-black/85 text-white flex items-center justify-center transition-all cursor-pointer border border-white/10 backdrop-blur-sm"
        aria-label="Close room details"
      >
        <X size={18} />
      </button>

      {/* Main Scrollable Content */}
      <div className="overflow-y-auto flex-grow overscroll-contain -m-[1px]" style={{ WebkitOverflowScrolling: 'touch' }}>
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-0">
          
          {/* Left Column: Interactive Image Gallery */}
          <div className="lg:col-span-7 bg-[#111] relative h-[320px] sm:h-[450px] lg:h-auto lg:min-h-[500px] -mt-[1px] -ml-[1px] -mr-[1px] lg:mr-0 lg:-mb-[1px] lg:-mt-[1px] lg:-ml-[1px]">
            {/* Active Display Image with Touch Swipe Support */}
            <div 
              className="w-full h-full overflow-hidden relative group touch-pan-y"
              onTouchStart={handleTouchStart}
              onTouchMove={handleTouchMove}
              onTouchEnd={handleTouchEnd}
            >
              <div 
                className="flex h-full w-full transition-transform duration-500 ease-out"
                style={{ 
                  transform: `translateX(-${activeIndex * 100}%)`,
                  willChange: 'transform' 
                }}
              >
                {allImages.map((img, idx) => (
                  <div key={idx} className="w-full h-full shrink-0 overflow-hidden relative">
                    <img
                      src={img}
                      alt={`${roomDetailData.name} - ${idx}`}
                      className="w-full h-full object-cover transition-transform duration-700 ease-out hover:scale-105"
                    />
                  </div>
                ))}
              </div>
              <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent pointer-events-none z-10" />

              {/* Room Type Switcher Arrows */}
              {onPrevRoomType && (
                <button
                  onClick={onPrevRoomType}
                  className="absolute left-3 top-1/2 -translate-y-1/2 z-30 w-10 h-10 rounded-full bg-black/50 hover:bg-black/80 text-white flex items-center justify-center transition-all cursor-pointer border border-white/20 shadow-lg"
                  title="Xem loại phòng trước"
                  aria-label="Previous room type"
                >
                  <ChevronLeft size={22} />
                </button>
              )}
              {onNextRoomType && (
                <button
                  onClick={onNextRoomType}
                  className="absolute right-3 top-1/2 -translate-y-1/2 z-30 w-10 h-10 rounded-full bg-black/50 hover:bg-black/80 text-white flex items-center justify-center transition-all cursor-pointer border border-white/20 shadow-lg"
                  title="Xem loại phòng kế tiếp"
                  aria-label="Next room type"
                >
                  <ChevronRight size={22} />
                </button>
              )}

              {/* Thin Carousel Indicators */}
              {allImages.length > 1 && (
                <div className="absolute bottom-6 left-1/2 -translate-x-1/2 flex gap-1.5 z-20">
                  {allImages.map((_, idx) => (
                    <button
                      key={idx}
                      onClick={() => setActiveIndex(idx)}
                      className={`h-1.5 rounded-full transition-all duration-300 cursor-pointer ${
                        activeIndex === idx 
                          ? 'w-10 bg-primary shadow-lg' 
                          : 'w-4 bg-white/40 hover:bg-white/70'
                      }`}
                      aria-label={`Xem ảnh ${idx + 1}`}
                    />
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Right Column: Premium Details & Amenities (lg:col-span-5) */}
          <div className="lg:col-span-5 p-6 sm:p-8 flex flex-col justify-between border-l border-slate-100">
            <div>
              <span className="text-[10px] font-semibold text-primary uppercase tracking-[0.2em] block mb-1">
                {roomDetailData.bedType || t('room_detail_premium_class', 'HẠNG PHÒNG THƯỢNG HẠNG')}
              </span>
              <h2 className="text-2xl sm:text-3xl font-semibold text-slate-900 uppercase tracking-wider leading-tight mb-3">
                {roomDetailData.name}
              </h2>
              <div className="h-0.5 w-16 bg-primary mb-5" />

              <p className="text-xs text-slate-500 font-medium leading-relaxed mb-6 whitespace-pre-line text-justify">
                {roomDetailData.description || t('room_detail_default_desc', 'Hệ thống phòng nghỉ đẳng cấp với các chi tiết kiến trúc độc đáo, mang lại sự sang trọng tinh tế và kỳ nghỉ tuyệt hảo chuẩn Elysian.')}
              </p>

              {/* Specs Grid */}
              <div className="grid grid-cols-2 gap-4 border-y border-slate-100 py-5 mb-6">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-full bg-slate-50 border border-slate-100 flex items-center justify-center text-primary shrink-0">
                    <Maximize size={16} />
                  </div>
                  <div>
                    <span className="text-[9px] text-slate-400 font-semibold uppercase tracking-wider block">{t('room_detail_area', 'Diện tích')}</span>
                    <span className="text-xs font-semibold text-slate-800 uppercase">{roomDetailData.area ? `${roomDetailData.area} m²` : t('room_detail_updating', 'Đang cập nhật')}</span>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-full bg-slate-50 border border-slate-100 flex items-center justify-center text-primary shrink-0">
                    <Users size={16} />
                  </div>
                  <div>
                    <span className="text-[9px] text-slate-400 font-semibold uppercase tracking-wider block">{t('room_detail_capacity', 'Sức chứa')}</span>
                    <span className="text-xs font-semibold text-slate-800 uppercase">
                      {roomDetailData.totalCapacity || ((roomDetailData.adultCapacity || 0) + (roomDetailData.childCapacity || 0))} {t('room_detail_guests_count', 'khách')}
                    </span>
                  </div>
                </div>
              </div>

              {/* Luxury Amenities List */}
              {roomDetailData.amenities && (
                <div className="mb-6">
                  <span className="block text-[10px] text-slate-400 font-semibold uppercase tracking-widest mb-3">{t('room_detail_amenities_title', 'Tiện ích đặc quyền')}</span>
                  <div className="grid grid-cols-2 gap-y-2 gap-x-4">
                    {roomDetailData.amenities.split(',').map((amenity, idx) => (
                      <div key={idx} className="flex items-center gap-2 text-xs font-semibold text-slate-700">
                        <Check size={14} className="text-primary shrink-0" />
                        <span className="truncate">{amenity.trim()}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Base Price Panel */}
            <div className="bg-slate-50 border border-slate-150 p-4 mb-2 flex items-center justify-between">
              <div>
                <span className="text-[9px] text-slate-400 font-semibold uppercase tracking-widest block">{t('room_detail_base_price', 'Giá khởi điểm')}</span>
                <span className="text-xl font-semibold text-primary leading-none">
                  {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(roomDetailData.basePrice)}
                </span>
                <span className="text-[10px] font-semibold text-slate-500 uppercase tracking-wider"> {t('room_detail_per_night', '/ đêm')}</span>
              </div>
            </div>
          </div>

        </div>

        {/* Booking Buttons Banner */}
        <div className="bg-slate-100 border-y border-slate-200 p-6 md:p-8 flex flex-col md:flex-row items-center gap-6 justify-between">
          <div className="text-center md:text-left">
            <h4 className="text-sm font-semibold text-slate-900 uppercase tracking-widest mb-1">{t('room_detail_cta_title', 'Hãy bắt đầu hành trình của bạn')}</h4>
            <p className="text-xs text-slate-500 font-semibold uppercase tracking-wider">{t('room_detail_cta_desc', 'Lựa chọn hình thức đặt phòng phù hợp nhất')}</p>
          </div>

          <div className="flex flex-col sm:flex-row gap-4 w-full md:w-auto">
            {/* Personal Booking */}
            <div className="parallelogram-btn bg-primary p-[2px] flex w-full sm:w-auto shrink-0">
              <button
                onClick={() => onBookingPersonal(roomDetailData)}
                className="parallelogram-btn bg-primary hover:bg-slate-100 text-white hover:text-primary px-8 py-3 text-xs font-semibold uppercase tracking-widest transition-all duration-500 ease-in-out cursor-pointer border-none flex flex-col items-center justify-center group w-full"
              >
                <span className="text-white group-hover:text-primary group-hover:translate-y-[-1px] transition-all duration-500 ease-in-out flex items-center gap-1.5 font-semibold">
                  {t('room_detail_book_personal', 'ĐẶT PHÒNG CÁ NHÂN')}
                </span>
                <span className="text-[8.5px] font-semibold text-white/90 group-hover:text-slate-500 block mt-0.5 tracking-wider font-sans leading-none transition-all duration-500 ease-in-out">{t('room_detail_book_personal_desc', 'Khách lẻ / Gia đình nhỏ')}</span>
              </button>
            </div>

            {/* Group Booking */}
            <div className="parallelogram-btn bg-primary p-[2px] flex w-full sm:w-auto shrink-0">
              <button
                onClick={() => onBookingGroup(roomDetailData)}
                className="parallelogram-btn bg-slate-100 hover:bg-primary text-primary hover:text-white px-8 py-3 text-xs font-semibold uppercase tracking-widest transition-all duration-500 ease-in-out cursor-pointer border-none flex flex-col items-center justify-center group w-full"
              >
                <span className="text-primary group-hover:text-white group-hover:translate-y-[-1px] transition-all duration-500 ease-in-out flex items-center gap-1.5 font-semibold">
                  {t('room_detail_book_group', 'ĐẶT PHÒNG NHÓM')}
                </span>
                <span className="text-[8.5px] font-semibold text-slate-500 group-hover:text-white/90 block mt-0.5 tracking-wider font-sans leading-none transition-all duration-500 ease-in-out">{t('room_detail_book_group_desc', 'Đoàn khách / Sự kiện lớn')}</span>
              </button>
            </div>
          </div>
        </div>

        {/* Reviews Dashboard (Takes full width inside the scroll view) */}
        <div className="px-6 sm:px-8 pb-12">
          <ReviewSection roomId={roomDetailData.id} showToast={showToast} />
        </div>

      </div>
    </div>
  );
}
