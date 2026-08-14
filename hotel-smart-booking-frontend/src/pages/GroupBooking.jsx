import { useState, useEffect } from 'react';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { getRoomAvailability, getRoomTypes } from '../services/roomService';
import { createGroupBooking } from '../services/groupBookingService';
import { getEkycProfile } from '../services/ekycService';
import { useToast, ToastContainer } from '../components/Toast';
import { useLanguage } from '../context/LanguageContext';

export default function GroupBooking({ setActivePage }) {
  const { t } = useLanguage();
  const { toasts, showToast, dismissToast } = useToast();
  
  const [currentStep, setCurrentStep] = useState(1);
  const [roomTypes, setRoomTypes] = useState([]);
  
  // Date Picker States
  const [startDate, setStartDate] = useState(null);
  const [endDate, setEndDate] = useState(null);
  
  // Group Booking States
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [quantity, setQuantity] = useState(2); // Minimum 2 for group booking
  const [adults, setAdults] = useState(2);
  const [childrenCount, setChildrenCount] = useState(0);
  
  // Step 2 & 3 States
  const [checkInMethod, setCheckInMethod] = useState('Manual'); // 'Manual' | 'Face ID' | 'QR Code'
  const [isEkycVerified, setIsEkycVerified] = useState(false);
  const [isEkycLoading, setIsEkycLoading] = useState(true);
  const [specialRequests, setSpecialRequests] = useState('');
  const [agreedToTerms, setAgreedToTerms] = useState(false);
  
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState({});
  const [availability, setAvailability] = useState(null);
  const [isAvailabilityLoading, setIsAvailabilityLoading] = useState(false);

  useEffect(() => {
    const fetchRooms = async () => {
      try {
        const response = await getRoomTypes('Active');
        if (response && response.data && response.data.content) {
          // Filter only active room types
          const activeRooms = response.data.content.filter(r => r.status === 'Active' || !r.status);
          setRoomTypes(activeRooms);
          
          // Check for pre-selected room from session storage
          const preSelectedStr = sessionStorage.getItem('bookingRoom');
          if (preSelectedStr) {
            const preSelected = JSON.parse(preSelectedStr);
            const matched = activeRooms.find(r => r.id === preSelected.id);
            if (matched) {
              setSelectedRoom(matched);
            } else if (activeRooms.length > 0) {
              setSelectedRoom(activeRooms[0]);
            }
          } else if (activeRooms.length > 0) {
            setSelectedRoom(activeRooms[0]);
          }
        }
      } catch (err) {
        console.error('Lỗi khi tải danh sách loại phòng:', err);
        showToast(t('group_toast_error_rooms', 'Không thể tải danh sách loại phòng'), 'error');
      }
    };
    
    fetchRooms();
  }, [showToast, t]);

  useEffect(() => {
    const fetchEkycStatus = async () => {
      try {
        const response = await getEkycProfile();
        setIsEkycVerified(
          String(response?.data?.status || '').toUpperCase() === 'VERIFIED',
        );
      } catch (error) {
        console.error('Không thể kiểm tra trạng thái eKYC:', error);
        setIsEkycVerified(false);
      } finally {
        setIsEkycLoading(false);
      }
    };

    fetchEkycStatus();
  }, []);

  useEffect(() => {
    if (!selectedRoom || !startDate || !endDate) {
      return undefined;
    }

    let isCurrentRequest = true;
    const toDateParam = (date) => {
      const yyyy = date.getFullYear();
      const mm = String(date.getMonth() + 1).padStart(2, '0');
      const dd = String(date.getDate()).padStart(2, '0');
      return `${yyyy}-${mm}-${dd}`;
    };

    const checkAvailability = async () => {
      setIsAvailabilityLoading(true);
      try {
        const response = await getRoomAvailability(
          selectedRoom.id,
          toDateParam(startDate),
          toDateParam(endDate),
        );
        if (isCurrentRequest) {
          setAvailability(response?.data ?? null);
        }
      } catch (error) {
        console.error('Unable to check room availability:', error);
        if (isCurrentRequest) {
          setAvailability(null);
        }
      } finally {
        if (isCurrentRequest) {
          setIsAvailabilityLoading(false);
        }
      }
    };

    checkAvailability();
    return () => {
      isCurrentRequest = false;
    };
  }, [selectedRoom, startDate, endDate]);

  const handleFaceIdSelection = () => {
    if (isEkycLoading) {
      showToast(t('group_ekyc_checking', 'Đang kiểm tra trạng thái eKYC, vui lòng chờ.'), 'info');
      return;
    }
    if (!isEkycVerified) {
      showToast(
        t(
          'group_ekyc_required',
          'Bạn phải hoàn thành đăng ký eKYC trước khi sử dụng check-in bằng FaceID.',
        ),
        'warning',
      );
      return;
    }
    setCheckInMethod('Face ID');
  };

  const handleQrCodeSelection = () => {
    if (isEkycLoading) {
      showToast(t('group_ekyc_checking', 'Đang kiểm tra trạng thái eKYC, vui lòng chờ.'), 'info');
      return;
    }
    if (!isEkycVerified) {
      showToast(
        'Vui lòng hoàn thành đăng ký eKYC trước khi sử dụng check-in bằng QR Code.',
        'warning',
      );
      return;
    }
    setCheckInMethod('QR Code');
  };

  const handleRoomSelection = (room) => {
    setSelectedRoom(room);
    setAvailability(null);
    setIsAvailabilityLoading(false);
    
    // Adjust quantities and capacities to match the new room type
    const adultCapPerRoom = room.adultCapacity || room.adultcapacity || 2;
    const childCapPerRoom = room.childCapacity || room.childcapacity || 1;
    
    // Ensure adult/child count fits within the new room capacities for the currently set quantity
    const maxAdults = adultCapPerRoom * quantity;
    const maxChildren = childCapPerRoom * quantity;
    
    if (adults > maxAdults) {
      setAdults(maxAdults);
    }
    if (childrenCount > maxChildren) {
      setChildrenCount(maxChildren);
    }
    
    showToast(`${t('group_toast_room_selected', 'Đã chọn loại phòng:')} ${room.name}`, 'info');
  };

  const handleQuantityChange = (newQty) => {
    if (newQty < 2) {
      showToast(t('group_toast_min_qty', 'Đặt phòng nhóm yêu cầu tối thiểu là 2 phòng.'), 'warning');
      return;
    }
    if (availability && newQty > availability.availableRooms) {
      showToast(`${availability.availableRooms} rooms available for the selected stay.`, 'warning');
      return;
    }
    setQuantity(newQty);
    
    // Dynamically adjust adults and children if they exceed new capacity limit
    if (selectedRoom) {
      const adultCapPerRoom = selectedRoom.adultCapacity || selectedRoom.adultcapacity || 2;
      const childCapPerRoom = selectedRoom.childCapacity || selectedRoom.childcapacity || 1;
      
      const maxAdults = adultCapPerRoom * newQty;
      const maxChildren = childCapPerRoom * newQty;
      
      if (adults > maxAdults) {
        setAdults(maxAdults);
      }
      if (childrenCount > maxChildren) {
        setChildrenCount(maxChildren);
      }
    }
  };

  // Calculate nights
  const calculateNights = () => {
    if (!startDate || !endDate) return 0;
    const diffTime = endDate - startDate;
    if (diffTime <= 0) return 0;
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  };

  const nights = calculateNights();
  const basePrice = selectedRoom ? selectedRoom.basePrice || selectedRoom.baseprice || 0 : 0;
  const totalAmount = basePrice * quantity * nights;
  const vatAmount = totalAmount * 0.1; // 10% VAT
  const finalAmount = totalAmount + vatAmount;

  const getAdultCapacity = (room) => room.adultCapacity || room.adultcapacity || 2;
  const getChildCapacity = (room) => room.childCapacity || room.childcapacity || 1;

  const formatDateString = (date) => {
    if (!date) return '';
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  };

  const hasEnoughRooms = availability && quantity <= availability.availableRooms;

  const handleStep1Submit = () => {
    const tempErrors = {};
    if (!startDate || !endDate) {
      tempErrors.dates = t('group_error_dates_required', 'Vui lòng chọn khoảng ngày nhận và trả phòng');
    }
    if (!selectedRoom) {
      tempErrors.room = t('group_error_room_required', 'Vui lòng chọn một loại phòng');
    }
    if (quantity < 2) {
      tempErrors.quantity = t('group_error_qty_required', 'Số lượng phòng cho đặt nhóm phải từ 2 trở lên');
    }
    
    if (!hasEnoughRooms) {
      tempErrors.quantity = availability
        ? `${availability.availableRooms} rooms available for the selected stay.`
        : 'Unable to confirm room availability. Please try again.';
    }

    if (selectedRoom) {
      const maxAdults = getAdultCapacity(selectedRoom) * quantity;
      const maxChildren = getChildCapacity(selectedRoom) * quantity;
      
      if (adults <= 0) {
        tempErrors.guests = t('group_error_adults_zero', 'Số người lớn phải lớn hơn 0');
      }
      if (adults > maxAdults) {
        tempErrors.guests = `${t('group_error_adults_max_1', 'Số người lớn')} (${adults}) ${t('group_error_adults_max_2', 'vượt quá sức chứa tối đa cho')} ${quantity} ${t('group_error_adults_max_3', 'phòng')} (${maxAdults} ${t('group_error_adults_max_4', 'người')})`;
      }
      if (childrenCount > maxChildren) {
        tempErrors.guests = `${t('group_error_children_max_1', 'Số trẻ em')} (${childrenCount}) ${t('group_error_children_max_2', 'vượt quá sức chứa tối đa cho')} ${quantity} ${t('group_error_children_max_3', 'phòng')} (${maxChildren} ${t('group_error_children_max_4', 'người')})`;
      }
    }

    setErrors(tempErrors);
    if (Object.keys(tempErrors).length === 0) {
      setCurrentStep(2);
    } else {
      showToast(t('group_toast_error_step1', 'Vui lòng kiểm tra lại thông tin bước 1.'), 'warning');
    }
  };

  const handleStep2Submit = () => {
    const tempErrors = {};
    if (!checkInMethod) {
      tempErrors.checkInMethod = t('group_error_checkin_required', 'Vui lòng chọn phương thức nhận phòng');
    }
    if (checkInMethod === 'Face ID' && !isEkycVerified) {
      tempErrors.checkInMethod = t(
        'group_error_ekyc_required',
        'Bạn chưa hoàn thành đăng ký eKYC để sử dụng FaceID',
      );
    }
    setErrors(tempErrors);
    if (Object.keys(tempErrors).length === 0) {
      setCurrentStep(3);
    }
  };

  const handleBookingSubmit = async () => {
    if (!agreedToTerms) {
      showToast(t('group_toast_error_terms', 'Bạn phải đồng ý với Điều khoản & Điều kiện để tiếp tục.'), 'warning');
      return;
    }

    setIsLoading(true);
    try {
      const payload = {
        roomTypeId: selectedRoom.id,
        checkInDate: formatDateString(startDate),
        checkOutDate: formatDateString(endDate),
        checkInMethod: checkInMethod === 'Face ID' ? 'FaceID' : checkInMethod,
        quantity: quantity,
        numberOfAdults: adults,
        numberOfChildren: childrenCount,
        specialRequests: specialRequests
      };

      const response = await createGroupBooking(payload);
      if (response && response.data) {
        showToast(t('group_toast_booking_success', 'Đặt phòng nhóm thành công!'), 'success');
        
        try {
          const existing = JSON.parse(localStorage.getItem('hotel_all_bookings') || '[]');
          const userObj = JSON.parse(localStorage.getItem('user') || '{}');
          const newBk = {
            ...response.data,
            guestName: userObj.fullName || userObj.name || 'Khách hàng The Iris',
            guestEmail: userObj.email || ''
          };
          existing.push(newBk);
          localStorage.setItem('hotel_all_bookings', JSON.stringify(existing));
        } catch (e) {
          console.error('Lỗi khi lưu đặt phòng nhóm vào danh sách dùng chung:', e);
        }

        sessionStorage.removeItem('bookingRoom');
        sessionStorage.setItem('selectedBookingId', response.data.bookingId);
        
        setTimeout(() => {
          setActivePage('booking-detail');
        }, 1500);
      }
    } catch (err) {
      const msg = err?.response?.data?.message || t('group_toast_booking_error', 'Có lỗi xảy ra khi tạo đặt phòng nhóm. Vui lòng thử lại.');
      showToast(msg, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      <style>{`
        .react-datepicker-wrapper {
          width: 100%;
        }
        .react-datepicker__input-container input {
          width: 100%;
          background: transparent;
          border-bottom: 1px solid #1a1c1c;
          padding: 8px 0;
          font-weight: 700;
          font-size: 14px;
          outline: none;
          cursor: pointer;
        }
        .react-datepicker__input-container input:focus {
          border-color: #a20513;
        }
        .react-datepicker {
          font-family: 'Montserrat', sans-serif;
          border: 1px solid #e4beba;
          border-radius: 0;
          box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
        }
        .react-datepicker__header {
          background-color: #ffffff;
          border-bottom: 1px solid #f3f3f4;
        }
        .react-datepicker__current-month {
          font-family: 'Montserrat', sans-serif;
          font-weight: 800;
          text-transform: uppercase;
          font-size: 11px;
          letter-spacing: 0.1em;
          color: #1a1c1c;
        }
        .react-datepicker__day-name {
          font-weight: 700;
          font-size: 10px;
          color: #a20513;
        }
        .react-datepicker__day--selected,
        .react-datepicker__day--in-selecting-range,
        .react-datepicker__day--in-range {
          background-color: #a20513 !important;
          color: white !important;
          border-radius: 0 !important;
        }
        .react-datepicker__day--keyboard-selected {
          background-color: #e4beba !important;
          color: #1a1c1c !important;
          border-radius: 0 !important;
        }
        .react-datepicker__day:hover {
          background-color: #ffe0dd !important;
          color: #a20513 !important;
          border-radius: 0 !important;
        }
      `}</style>
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />
      
      {/* Banner Area */}
      <div className="w-full bg-slate-950 text-white pt-32 pb-16 px-4 md:px-margin-desktop text-center relative overflow-hidden">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(162,5,19,0.15)_0%,transparent_100%)] pointer-events-none" />
        <div className="max-w-4xl mx-auto relative z-10 animate-fade-in-up">
          <span className="text-[10px] font-black tracking-[0.4em] uppercase text-primary mb-2 block">{t('group_banner_sub', 'DÀNH CHO ĐOÀN & DOANH NGHIỆP')}</span>
          <h1 className="font-headline-xl text-3xl md:text-5xl font-black uppercase tracking-wider mb-4 leading-none">
            {t('group_banner_title', 'ĐẶT PHÒNG NHÓM THE IRIS')}
          </h1>
          <p className="text-xs md:text-sm text-slate-300 font-bold uppercase tracking-widest max-w-2xl mx-auto">
            {t('group_banner_desc', 'Trải nghiệm dịch vụ lưu trú thông minh, tiện nghi vượt trội và các chương trình ưu đãi dành riêng cho đoàn từ 2 phòng trở lên.')}
          </p>
        </div>
      </div>


      <div className="w-full min-h-screen pb-24 bg-gray-50 flex items-start justify-center px-4 font-['Montserrat']">
        <div className="max-w-7xl w-full grid grid-cols-1 lg:grid-cols-12 gap-8 text-left -mt-8 relative z-20">
          
          {/* Left Column: Wizard Steps (col-span-8) */}
          <div className="lg:col-span-8 bg-white p-6 md:p-10 border border-outline-variant shadow-lg flex flex-col justify-between">
            <div>
              {/* Navigation Back */}
              {currentStep === 1 ? (
                <button 
                  onClick={() => setActivePage('home')}
                  className="mb-6 w-fit text-xs text-secondary hover:text-primary uppercase tracking-widest font-bold flex items-center gap-1 cursor-pointer bg-transparent border-none"
                >
                  <span className="material-symbols-outlined text-sm">arrow_back</span> {t('booking_btn_cancel_back', 'Hủy & Quay lại')}
                </button>
              ) : (
                <button 
                  onClick={() => setCurrentStep(prev => prev - 1)}
                  className="mb-6 w-fit text-xs text-secondary hover:text-primary uppercase tracking-widest font-bold flex items-center gap-1 cursor-pointer bg-transparent border-none"
                >
                  <span className="material-symbols-outlined text-sm">arrow_back</span> {t('booking_btn_back_step', 'Quay lại bước')} {currentStep - 1}
                </button>
              )}

              {/* Progress Tracker */}
              <div className="flex justify-between items-center mb-8 border-b border-gray-100 pb-4">
                <div className="flex items-center gap-4 md:gap-6">
                  <div className="flex items-center gap-2">
                    <span className={`w-6 h-6 flex items-center justify-center text-[10px] font-black border ${currentStep >= 1 ? 'bg-primary border-primary text-white' : 'border-slate-300 text-slate-400'}`}>1</span>
                    <span className={`text-[10px] font-black uppercase tracking-wider hidden sm:inline ${currentStep === 1 ? 'text-primary' : 'text-slate-400'}`}>{t('group_step1_label', 'Thông tin & Chọn phòng')}</span>
                  </div>
                  <span className="h-px w-6 bg-slate-350 hidden sm:inline" />
                  <div className="flex items-center gap-2">
                    <span className={`w-6 h-6 flex items-center justify-center text-[10px] font-black border ${currentStep >= 2 ? 'bg-primary border-primary text-white' : 'border-slate-300 text-slate-400'}`}>2</span>
                    <span className={`text-[10px] font-black uppercase tracking-wider hidden sm:inline ${currentStep === 2 ? 'text-primary' : 'text-slate-400'}`}>{t('booking_step2_label', 'Hình thức Check-in')}</span>
                  </div>
                  <span className="h-px w-6 bg-slate-350 hidden sm:inline" />
                  <div className="flex items-center gap-2">
                    <span className={`w-6 h-6 flex items-center justify-center text-[10px] font-black border ${currentStep >= 3 ? 'bg-primary border-primary text-white' : 'border-slate-300 text-slate-400'}`}>3</span>
                    <span className={`text-[10px] font-black uppercase tracking-wider hidden sm:inline ${currentStep === 3 ? 'text-primary' : 'text-slate-400'}`}>{t('booking_step3_label', 'Xác nhận')}</span>
                  </div>
                </div>
                <div className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">{t('booking_step_progress_text', 'Bước')} {currentStep} / 3</div>
              </div>

              {/* STEP 1: Date & Dynamic Room list */}
              {currentStep === 1 && (
                <div className="space-y-8 animate-scale-in">
                  <h3 className="font-headline-lg text-lg text-primary uppercase italic tracking-wider m-0">{t('group_step1_title', 'Bước 1: Thông tin lưu trú & Chọn loại phòng')}</h3>
                  
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {/* Stay Dates */}
                    <div className="space-y-2">
                      <label className="block text-xs font-bold text-secondary uppercase tracking-widest">{t('booking_duration_label', 'Thời gian lưu trú (Check-in - Check-out)')}</label>
                      <DatePicker
                        selectsRange={true}
                        startDate={startDate}
                        endDate={endDate}
                        onChange={(dates) => {
                          const [start, end] = dates;
                          setStartDate(start);
                          setEndDate(end);
                          setAvailability(null);
                          setIsAvailabilityLoading(false);
                        }}
                        minDate={new Date()}
                        placeholderText={t('booking_dates_placeholder', 'Chọn khoảng ngày nhận và trả phòng')}
                        isClearable={true}
                        className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary"
                      />
                      {errors.dates && <p className="text-xs text-error font-medium mt-1">{errors.dates}</p>}
                    </div>

                    {/* Quantity of Rooms (Min 2) */}
                    <div className="space-y-2">
                      <label className="block text-xs font-bold text-secondary uppercase tracking-widest">{t('group_qty_label', 'Số lượng phòng đặt (Tối thiểu 2)')}</label>
                      <div className="flex items-center gap-3 border border-slate-200 p-2.5 w-full md:w-48 bg-slate-50/50">
                        <button
                          type="button"
                          onClick={() => handleQuantityChange(quantity - 1)}
                          className="w-8 h-8 border border-slate-350 bg-white text-slate-800 hover:bg-slate-100 font-black flex items-center justify-center cursor-pointer"
                        >
                          -
                        </button>
                        <span className="text-sm font-black w-8 text-center">{quantity}</span>
                        <button
                          type="button"
                          onClick={() => handleQuantityChange(quantity + 1)}
                          disabled={isAvailabilityLoading || Boolean(availability && quantity >= availability.availableRooms)}
                          className="w-8 h-8 border border-slate-350 bg-white text-slate-800 hover:bg-slate-100 font-black flex items-center justify-center cursor-pointer disabled:cursor-not-allowed disabled:opacity-40"
                        >
                          +
                        </button>
                      </div>
                      {startDate && endDate && (
                        <p className={`text-xs font-semibold mt-2 ${hasEnoughRooms ? 'text-emerald-700' : 'text-error'}`}>
                          {isAvailabilityLoading
                            ? t('booking_checking_availability', 'Checking availability...')
                            : availability
                              ? `${availability.availableRooms} rooms available.`
                              : 'Availability could not be checked. Please try again.'}
                        </p>
                      )}
                      {errors.quantity && <p className="text-xs text-error font-medium mt-1">{errors.quantity}</p>}
                    </div>
                  </div>

                  {/* DYNAMIC ROOM LIST */}
                  <div className="space-y-4 pt-4 border-t border-gray-100">
                    <div className="flex justify-between items-center">
                      <h4 className="text-xs font-black text-slate-900 uppercase tracking-widest">
                        {t('group_room_list_title', 'Danh Sách Loại Phòng Khả Dụng (Dynamic Room List)')}
                      </h4>
                      <span className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">{t('group_room_list_subtitle', 'Chọn 1 loại phòng phù hợp')}</span>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      {roomTypes.map((room) => {
                        const isSelected = selectedRoom?.id === room.id;
                        const adultCap = getAdultCapacity(room);
                        const childCap = getChildCapacity(room);
                        
                        return (
                          <div 
                            key={room.id}
                            onClick={() => handleRoomSelection(room)}
                            className={`group border transition-all duration-300 flex flex-col justify-between cursor-pointer overflow-hidden ${
                              isSelected 
                                ? 'border-primary ring-2 ring-primary/20 shadow-xl' 
                                : 'border-slate-200 hover:border-slate-400 hover:shadow-md'
                            }`}
                          >
                            <div className="relative h-44 overflow-hidden bg-slate-100">
                              <img 
                                src={room.primaryImageUrl || 'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=600&q=80'}
                                alt={room.name}
                                className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                              />
                              <div className="absolute top-3 right-3 bg-black/60 backdrop-blur-sm px-2.5 py-1 text-[9px] font-black uppercase text-white tracking-widest">
                                {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(room.basePrice || room.baseprice || 0)} / {t('booking_summary_nights', 'Đêm')}
                              </div>
                              {isSelected && (
                                <div className="absolute top-3 left-3 bg-primary px-2.5 py-1 text-[9px] font-black uppercase text-white tracking-widest flex items-center gap-1 shadow-md">
                                  <span className="material-symbols-outlined text-[10px] font-bold">check</span> {t('group_room_selected_badge', 'Đang chọn')}
                                </div>
                              )}
                            </div>
                            
                            <div className="p-4 flex-grow flex flex-col justify-between">
                              <div>
                                <h5 className="text-sm font-black text-slate-900 uppercase tracking-wider mb-1">
                                  {room.name}
                                </h5>
                                <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mb-3">
                                  {room.bedType || 'Giường cao cấp'} • {room.roomSize || room.roomsize || '32'} m²
                                </p>
                                <div className="flex flex-wrap gap-2 mb-4">
                                  <span className="bg-slate-100 text-slate-600 text-[9px] font-bold uppercase tracking-widest px-2 py-0.5">
                                    {t('group_capacity_adults_prefix', 'NL/Phòng:')} {adultCap}
                                  </span>
                                  <span className="bg-slate-100 text-slate-600 text-[9px] font-bold uppercase tracking-widest px-2 py-0.5">
                                    {t('group_capacity_children_prefix', 'TE/Phòng:')} {childCap}
                                  </span>
                                </div>
                              </div>

                              {/* Capacity information dynamically calculated */}
                              <div className="border-t border-slate-100 pt-3 flex justify-between items-center text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                                <span>{t('group_max_capacity_prefix', 'Tối đa (')} {quantity} {t('group_max_capacity_rooms', 'phòng):')}</span>
                                <span className="text-slate-800">
                                  {adultCap * quantity} NL / {childCap * quantity} TE
                                </span>
                              </div>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                    {errors.room && <p className="text-xs text-error font-medium mt-1">{errors.room}</p>}
                  </div>

                  {/* GUESTS COUNTERS */}
                  {selectedRoom && (
                    <div className="space-y-4 border-t border-gray-100 pt-6">
                      <div className="flex justify-between items-center">
                        <label className="block text-xs font-bold text-secondary uppercase tracking-widest">
                          {t('group_guests_total_label', 'Tổng số khách của đoàn')} ({quantity} {t('group_max_capacity_rooms', 'phòng')})
                        </label>
                        <span className="text-[10px] font-black text-primary uppercase tracking-wider bg-primary/5 px-2.5 py-1">
                          {t('group_guests_capacity_prefix', 'Sức chứa đoàn:')} {getAdultCapacity(selectedRoom) * quantity} {t('booking_summary_nl', 'NL')}, {getChildCapacity(selectedRoom) * quantity} {t('booking_summary_te', 'TE')}
                        </span>
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                        {/* Adults */}
                        <div className="flex justify-between items-center border border-slate-200 p-3 bg-slate-50/30">
                          <div>
                            <span className="text-xs font-extrabold uppercase text-slate-700 block">{t('group_guests_adults_label', 'Người lớn đoàn')}</span>
                            <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">{t('group_guests_adults_desc', 'Yêu cầu từ 2 người trở lên')}</span>
                          </div>
                          <div className="flex items-center gap-3">
                            <button
                              type="button"
                              onClick={() => setAdults(prev => Math.max(quantity, prev - 1))}
                              className="w-8 h-8 border border-slate-300 bg-white text-slate-700 hover:bg-slate-100 font-black flex items-center justify-center cursor-pointer"
                            >
                              -
                            </button>
                            <span className="text-sm font-black w-4 text-center">{adults}</span>
                            <button
                              type="button"
                              onClick={() => {
                                const max = getAdultCapacity(selectedRoom) * quantity;
                                if (adults < max) {
                                  setAdults(prev => prev + 1);
                                } else {
                                  showToast(`${t('group_error_adults_limit', 'Sức chứa tối đa của')} ${quantity} ${t('group_error_adults_limit_2', 'phòng là')} ${max} ${t('group_error_adults_limit_3', 'người lớn.')}`, 'warning');
                                }
                              }}
                              className="w-8 h-8 border border-slate-300 bg-white text-slate-700 hover:bg-slate-100 font-black flex items-center justify-center cursor-pointer"
                            >
                              +
                            </button>
                          </div>
                        </div>

                        {/* Children */}
                        <div className="flex justify-between items-center border border-slate-200 p-3 bg-slate-50/30">
                          <div>
                            <span className="text-xs font-extrabold uppercase text-slate-700 block">{t('group_guests_children_label', 'Trẻ em đoàn')}</span>
                            <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">{t('booking_children_sub', 'Dưới 12 tuổi')}</span>
                          </div>
                          <div className="flex items-center gap-3">
                            <button
                              type="button"
                              onClick={() => setChildrenCount(prev => Math.max(0, prev - 1))}
                              className="w-8 h-8 border border-slate-350 bg-white text-slate-700 hover:bg-slate-100 font-black flex items-center justify-center cursor-pointer"
                            >
                              -
                            </button>
                            <span className="text-sm font-black w-4 text-center">{childrenCount}</span>
                            <button
                              type="button"
                              onClick={() => {
                                const max = getChildCapacity(selectedRoom) * quantity;
                                if (childrenCount < max) {
                                  setChildrenCount(prev => prev + 1);
                                } else {
                                  showToast(`${t('group_error_children_limit', 'Sức chứa tối đa của')} ${quantity} ${t('group_error_children_limit_2', 'phòng là')} ${max} ${t('group_error_children_limit_3', 'trẻ em.')}`, 'warning');
                                }
                              }}
                              className="w-8 h-8 border border-slate-350 bg-white text-slate-800 hover:bg-slate-100 font-black flex items-center justify-center cursor-pointer"
                            >
                              +
                            </button>
                          </div>
                        </div>
                      </div>
                      {errors.guests && <p className="text-xs text-error font-medium mt-1">{errors.guests}</p>}
                    </div>
                  )}

                  <button
                    type="button"
                    onClick={handleStep1Submit}
                    disabled={!selectedRoom || !startDate || !endDate || isAvailabilityLoading || !hasEnoughRooms}
                    className="w-full bg-primary text-on-primary font-bold py-4 uppercase tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center h-12 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {t('group_btn_continue_checkin', 'Tiếp tục sang phương thức Check-in')}
                  </button>
                </div>
              )}

              {/* STEP 2: CHECK-IN METHOD */}
              {currentStep === 2 && (
                <div className="space-y-6 animate-scale-in">
                  <h3 className="font-headline-lg text-lg text-primary uppercase italic tracking-wider m-0">{t('group_step2_title', 'Bước 2: Phương thức nhận phòng đoàn')}</h3>
                  <p className="text-secondary text-xs font-bold uppercase tracking-widest">
                    {t('group_step2_subtitle', 'Chọn cách thức làm thủ tục check-in nhanh nhất cho cả đoàn tại The Iris Hotels')}
                  </p>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    {/* QR Code */}
                    <div 
                      onClick={handleQrCodeSelection}
                      aria-disabled={isEkycLoading || !isEkycVerified}
                      className={`border p-6 flex flex-col items-center justify-center text-center cursor-pointer transition-all duration-300 ${
                        checkInMethod === 'QR Code' 
                          ? 'border-primary bg-primary/5 text-primary shadow-md' 
                          : isEkycLoading || !isEkycVerified
                            ? 'border-slate-200 text-slate-400 bg-slate-100 cursor-not-allowed opacity-70'
                            : 'border-slate-200 hover:border-primary/50 text-slate-700 bg-white'
                      }`}
                    >
                      <span className="material-symbols-outlined text-3xl mb-3">qr_code_2</span>
                      <span className="text-[12px] font-black uppercase tracking-wider block">{t('group_checkin_qr_title', 'Mã QR Đoàn')}</span>
                      <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider mt-1.5">{t('group_checkin_qr_desc', 'Tự động check-in tại Kiosk')}</span>
                    </div>

                    {/* FaceID */}
                    <div 
                      onClick={handleFaceIdSelection}
                      aria-disabled={isEkycLoading || !isEkycVerified}
                      className={`border p-6 flex flex-col items-center justify-center text-center cursor-pointer transition-all duration-300 ${
                        checkInMethod === 'Face ID' 
                          ? 'border-primary bg-primary/5 text-primary shadow-md' 
                          : isEkycLoading || !isEkycVerified
                            ? 'border-slate-200 text-slate-400 bg-slate-100 cursor-not-allowed opacity-70'
                            : 'border-slate-200 hover:border-primary/50 text-slate-700 bg-white'
                      }`}
                    >
                      <span className="material-symbols-outlined text-3xl mb-3">face</span>
                      <span className="text-[12px] font-black uppercase tracking-wider block">{t('group_checkin_face_title', 'Face ID eKYC')}</span>
                      <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider mt-1.5">
                        {isEkycLoading
                          ? t('group_ekyc_checking_short', 'Đang kiểm tra eKYC')
                          : isEkycVerified
                            ? t('group_checkin_face_desc', 'Xác minh chủ booking')
                            : t('group_ekyc_required_short', 'Cần đăng ký eKYC trước')}
                      </span>
                    </div>

                    {/* Manual */}
                    <div 
                      onClick={() => setCheckInMethod('Manual')}
                      className={`border p-6 flex flex-col items-center justify-center text-center cursor-pointer transition-all duration-300 ${
                        checkInMethod === 'Manual' 
                          ? 'border-primary bg-primary/5 text-primary shadow-md' 
                          : 'border-slate-200 hover:border-primary/50 text-slate-700 bg-white'
                      }`}
                    >
                      <span className="material-symbols-outlined text-3xl mb-3">hotel_class</span>
                      <span className="text-[12px] font-black uppercase tracking-wider block">{t('group_checkin_manual_title', 'Tại quầy (Manual)')}</span>
                      <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider mt-1.5">{t('group_checkin_manual_desc', 'Hỗ trợ nhận phòng trực tiếp')}</span>
                    </div>
                  </div>

                  {/* Special Requests */}
                  <div className="space-y-2 border-t border-gray-100 pt-6">
                    <label className="block text-xs font-bold text-secondary uppercase tracking-widest">
                      {t('booking_special_requests_label', 'Yêu Cầu Đặc Biệt (Tùy chọn)')}
                    </label>
                    <textarea 
                      rows="4"
                      value={specialRequests}
                      onChange={(e) => setSpecialRequests(e.target.value)}
                      placeholder={t('group_special_requests_placeholder', 'Nhập ghi chú cụ thể: sắp xếp các phòng gần nhau, yêu cầu hóa đơn đỏ cho doanh nghiệp, hỗ trợ tổ chức tiệc nhẹ hoặc phòng hội nghị...')}
                      className="w-full bg-transparent border border-slate-200 p-3 font-bold text-sm outline-none focus:border-primary resize-none"
                    />
                  </div>

                  <button
                    type="button"
                    onClick={handleStep2Submit}
                    className="w-full bg-primary text-on-primary font-bold py-4 uppercase tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center h-12"
                  >
                    {t('booking_btn_continue_summary', 'Tiếp tục sang bước tổng kết')}
                  </button>
                </div>
              )}

              {/* STEP 3: SUMMARY & CONFIRM */}
              {currentStep === 3 && (
                <div className="space-y-6 animate-scale-in">
                  <h3 className="font-headline-lg text-lg text-primary uppercase italic tracking-wider m-0">{t('booking_step3_title', 'Bước 3: Tổng kết & Điều khoản')}</h3>
                  
                  {/* Terms & Conditions details box */}
                  <div className="bg-slate-50 border border-slate-200 p-5 space-y-3">
                    <span className="block text-xs font-black text-slate-900 uppercase tracking-wider border-b border-slate-200 pb-1.5">
                      {t('group_tc_title', 'Chính sách đặt phòng nhóm & Sự kiện')}
                    </span>
                    <div className="text-[10px] text-slate-500 font-semibold uppercase tracking-wider leading-relaxed max-h-48 overflow-y-auto pr-2">
                      <p className="mb-2">{t('group_tc_1', '1. QUY MÔ ĐOÀN: Chương trình áp dụng với đặt phòng từ 2 phòng trở lên cùng ngày lưu trú.')}</p>
                      <p className="mb-2">{t('group_tc_2', '2. XÁC NHẬN ĐẶT PHÒNG: Thông tin đặt phòng sẽ được hệ thống Smart Hotel xử lý và nhân viên chăm sóc khách hàng The Iris sẽ gọi điện hỗ trợ trực tiếp điều phối phòng phù hợp nhất.')}</p>
                      <p className="mb-2">{t('group_tc_3', '3. HỦY / ĐỔI LỊCH: Yêu cầu hủy đặt phòng nhóm miễn phí cần được thông báo trước tối thiểu 7 ngày nhận phòng. Các thay đổi trễ hơn sẽ chịu phụ thu theo thỏa thuận.')}</p>
                      <p className="mb-2">{t('group_tc_4', '4. THANH TOÁN: Các điều khoản thanh toán chiết khấu sẽ được ghi rõ trong hợp đồng đoàn.')}</p>
                    </div>
                  </div>

                  {/* Accept terms checkbox */}
                  <label className="flex items-start gap-3 cursor-pointer select-none">
                    <input 
                      type="checkbox"
                      checked={agreedToTerms}
                      onChange={(e) => setAgreedToTerms(e.target.checked)}
                      className="mt-1 accent-primary w-4 h-4 cursor-pointer"
                    />
                    <span className="text-xs font-bold text-slate-700 leading-tight">
                      {t('group_tc_agree_text', 'Tôi đại diện cho đoàn đã đọc, hiểu và đồng ý hoàn toàn với Chính sách đặt phòng nhóm của The Iris Hotels.')}
                    </span>
                  </label>

                  <button
                    type="button"
                    onClick={handleBookingSubmit}
                    disabled={isLoading || !agreedToTerms}
                    className="w-full bg-primary text-on-primary font-bold py-4 uppercase tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-2 h-12 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {isLoading ? (
                      <>
                        <span className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                        {t('group_btn_processing', 'ĐANG TẠO HỒ SƠ ĐOÀN...')}
                      </>
                    ) : t('group_btn_confirm', 'XÁC NHẬN ĐẶT PHÒNG NHÓM')}
                  </button>
                </div>
              )}
            </div>
          </div>

          {/* Right Column: Booking Summary - REAL-TIME (col-span-4) */}
          <div className="lg:col-span-4 bg-white border border-outline-variant shadow-lg p-6 md:p-8 flex flex-col justify-between h-fit">
            <div>
              <h3 className="text-xs font-black text-slate-900 uppercase tracking-widest mb-4 border-b border-gray-100 pb-2">
                {t('group_summary_title', 'Tóm Tắt Chi Phí Đoàn')}
              </h3>

              {selectedRoom ? (
                <div className="space-y-4">
                  {/* Room Image */}
                  <div className="w-full h-40 overflow-hidden bg-slate-100 border border-slate-200">
                    <img 
                      src={selectedRoom.primaryImageUrl || 'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=600&q=80'} 
                      alt={selectedRoom.name}
                      className="w-full h-full object-cover"
                    />
                  </div>

                  {/* Room Title */}
                  <div>
                    <span className="text-[9px] font-black text-primary uppercase tracking-widest block mb-0.5">
                      {selectedRoom.bedType || t('group_summary_room_type_default', 'Phòng nghỉ đoàn cao cấp')}
                    </span>
                    <h4 className="text-base font-black text-slate-950 uppercase tracking-wider">
                      {selectedRoom.name}
                    </h4>
                  </div>

                  {/* Details list */}
                  <div className="space-y-2.5 border-t border-b border-gray-100 py-4 text-xs font-bold text-slate-700">
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('group_summary_price_per_night', 'Giá mỗi phòng/đêm:')}</span>
                      <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(basePrice)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('group_summary_room_qty', 'Số lượng phòng:')}</span>
                      <span className="text-primary font-black">{quantity} {t('group_max_capacity_rooms', 'phòng')}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('booking_summary_duration', 'Thời gian')}:</span>
                      {startDate && endDate ? (
                        <span>{formatDateString(startDate)} {t('booking_summary_date_to', 'đến')} {formatDateString(endDate)} ({nights} {t('booking_summary_nights', 'đêm')})</span>
                      ) : (
                        <span className="text-slate-400 italic font-medium">{t('booking_summary_no_date', 'Chưa chọn ngày')}</span>
                      )}
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('group_summary_members', 'Thành viên đoàn:')}</span>
                      <span>{adults} {t('booking_summary_nl', 'NL')} • {childrenCount} {t('booking_summary_te', 'TE')}</span>
                    </div>
                    <div className="flex justify-between flex-wrap gap-1">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('booking_summary_checkin_method', 'Hình thức check-in:')}</span>
                      <span className="text-slate-900 uppercase">
                        {checkInMethod === 'Manual' ? t('booking_checkin_manual_option', 'Quầy lễ tân') : checkInMethod === 'Face ID' ? t('ekyc_status_verified_title', 'Face ID eKYC') : t('booking_checkin_qr_option', 'Mã QR')}
                      </span>
                    </div>
                  </div>

                  {/* Price breakdown */}
                  <div className="space-y-2 pt-2 text-xs font-bold text-slate-700">
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('group_summary_subtotal_prefix', 'Tạm tính (')} {quantity} {t('group_summary_subtotal_suffix', 'phòng):')}</span>
                      <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(totalAmount)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('booking_summary_vat', 'Thuế VAT (10%):')}</span>
                      <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(vatAmount)}</span>
                    </div>
                    <div className="flex justify-between border-t border-slate-900 pt-3 text-xs">
                      <span className="font-black text-slate-900 uppercase tracking-wider">{t('booking_summary_total', 'TỔNG CỘNG:')}</span>
                      <span className="font-black text-primary text-sm">
                        {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(finalAmount)}
                      </span>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="py-12 text-center text-slate-400 font-bold uppercase text-[10px] tracking-wider">
                  {t('group_summary_empty', 'Vui lòng chọn loại phòng khả dụng')}
                </div>
              )}
            </div>

            <div className="mt-8 bg-slate-50 border border-slate-100 p-4 text-[9px] text-slate-500 font-semibold leading-relaxed uppercase tracking-wider">
              {t('group_notice_discount', 'The Iris hỗ trợ chiết khấu đặc biệt cho doanh nghiệp & sự kiện quy mô lớn. Vui lòng ghi chú trong yêu cầu đặc biệt.')}
            </div>
          </div>
          
        </div>
      </div>
    </>
  );
}
