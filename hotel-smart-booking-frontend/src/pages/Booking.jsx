import { useState, useEffect } from 'react';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { getRoomTypes } from '../services/roomService';
import { createBooking } from '../services/bookingService';
import { useToast, ToastContainer } from '../components/Toast';
import { useLanguage } from '../context/LanguageContext';

export default function Booking({ setActivePage }) {
  const { toasts, showToast, dismissToast } = useToast();
  const { t } = useLanguage();
  
  const [currentStep, setCurrentStep] = useState(1);
  const [roomTypes, setRoomTypes] = useState([]);
  const [selectedRoom, setSelectedRoom] = useState(null);
  
  // Date Picker States
  const [startDate, setStartDate] = useState(null);
  const [endDate, setEndDate] = useState(null);
  
  // Guest Stepper States
  const [adults, setAdults] = useState(1);
  const [childrenCount, setChildrenCount] = useState(0);
  
  // Availability Check State
  const [isCheckingAvailability, setIsCheckingAvailability] = useState(false);
  const [isAvailable, setIsAvailable] = useState(null); // null | true | false
  
  // Step 2 & 3 States
  const [checkInMethod, setCheckInMethod] = useState('Manual'); // 'Manual' | 'FaceID' | 'QR Code'
  const [specialRequests, setSpecialRequests] = useState('');
  const [agreedToTerms, setAgreedToTerms] = useState(false);
  
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    const fetchRooms = async () => {
      try {
        const response = await getRoomTypes();
        if (response && response.data && response.data.content) {
          setRoomTypes(response.data.content);
          
          // Check for pre-selected room
          const preSelectedStr = sessionStorage.getItem('bookingRoom');
          if (preSelectedStr) {
            const preSelected = JSON.parse(preSelectedStr);
            const matched = response.data.content.find(r => r.id === preSelected.id);
            if (matched) {
              setSelectedRoom(matched);
            } else {
              setSelectedRoom(response.data.content[0]);
            }
          } else {
            setSelectedRoom(response.data.content[0]);
          }
        }
      } catch (err) {
        console.error('Lỗi khi tải danh sách loại phòng:', err);
        showToast('Không thể tải danh sách loại phòng', 'error');
      }
    };
    
    fetchRooms();
  }, [showToast]);

  // Reset availability status when dates or room changes
  useEffect(() => {
    setTimeout(() => {
      setIsAvailable(null);
    }, 0);
  }, [startDate, endDate, selectedRoom]);

  const handleRoomChange = (roomId) => {
    const room = roomTypes.find(r => r.id === parseInt(roomId));
    setSelectedRoom(room);
    
    // Reset guest count if it exceeds new capacity
    const maxCapacity = room ? room.totalCapacity || ((room.adultCapacity || 2) + (room.childCapacity || 1)) : 3;
    if (adults + childrenCount > maxCapacity) {
      setAdults(1);
      setChildrenCount(0);
    }
  };

  const handleDateChange = (dates) => {
    const [start, end] = dates;
    setStartDate(start);
    setEndDate(end);
  };

  const checkAvailability = () => {
    if (!startDate || !endDate) {
      showToast('Vui lòng chọn khoảng ngày lưu trú.', 'error');
      return;
    }
    
    setIsCheckingAvailability(true);
    // Simulate API call to check room availability
    setTimeout(() => {
      setIsCheckingAvailability(false);
      setIsAvailable(true);
      showToast('Phòng vẫn còn trống trong khoảng thời gian này!', 'success');
    }, 1200);
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
  const totalAmount = basePrice * nights;
  const vatAmount = totalAmount * 0.1; // 10% VAT
  const finalAmount = totalAmount + vatAmount;

  const maxCapacity = selectedRoom ? selectedRoom.totalCapacity || ((selectedRoom.adultCapacity || 2) + (selectedRoom.childCapacity || 1)) : 3;
  const totalGuests = adults + childrenCount;

  // Formatting dates to YYYY-MM-DD for backend API
  const formatDateString = (date) => {
    if (!date) return '';
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  };

  const handleStep1Submit = () => {
    const tempErrors = {};
    if (!startDate || !endDate) {
      tempErrors.dates = 'Vui lòng chọn khoảng ngày nhận và trả phòng';
    }
    if (totalGuests <= 0) {
      tempErrors.guests = 'Số lượng khách phải lớn hơn 0';
    }
    if (totalGuests > maxCapacity) {
      tempErrors.guests = `Tổng số khách (${totalGuests}) vượt quá sức chứa tối đa của phòng (${maxCapacity} người)`;
    }
    if (isAvailable === false) {
      tempErrors.availability = 'Khoảng thời gian này đã hết phòng trống';
    }

    setErrors(tempErrors);
    if (Object.keys(tempErrors).length === 0) {
      if (isAvailable === null) {
        // Automatically mark as available if they didn't manually check
        setIsAvailable(true);
      }
      setCurrentStep(2);
    } else {
      showToast('Vui lòng kiểm tra lại thông tin bước 1.', 'error');
    }
  };

  const handleStep2Submit = () => {
    const tempErrors = {};
    if (!checkInMethod) {
      tempErrors.checkInMethod = 'Vui lòng chọn phương thức nhận phòng';
    }
    setErrors(tempErrors);
    if (Object.keys(tempErrors).length === 0) {
      setCurrentStep(3);
    }
  };

  const handleBookingSubmit = async () => {
    if (!agreedToTerms) {
      showToast('Bạn phải đồng ý với Điều khoản & Điều kiện để tiếp tục.', 'error');
      return;
    }

    setIsLoading(true);
    try {
      const bookingPayload = {
        roomTypeId: selectedRoom.id,
        checkInDate: formatDateString(startDate),
        checkOutDate: formatDateString(endDate),
        checkInMethod: checkInMethod === 'FaceID' ? 'Face Recognition' : checkInMethod,
        specialRequests
      };

      const response = await createBooking(bookingPayload);
      if (response && response.data) {
        showToast('Đặt phòng thành công!', 'success');
        
        try {
          const existing = JSON.parse(localStorage.getItem('hotel_all_bookings') || '[]');
          const userObj = JSON.parse(localStorage.getItem('user') || '{}');
          const newBk = {
            ...response.data,
            guestName: userObj.fullName || userObj.name || 'Khách hàng Elysian',
            guestEmail: userObj.email || ''
          };
          existing.push(newBk);
          localStorage.setItem('hotel_all_bookings', JSON.stringify(existing));
        } catch (e) {
          console.error('Lỗi khi lưu đặt phòng vào danh sách dùng chung:', e);
        }

        sessionStorage.removeItem('bookingRoom');
        sessionStorage.setItem('selectedBookingId', response.data.bookingId);
        
        setTimeout(() => {
          setActivePage('booking-detail');
        }, 1500);
      }
    } catch (err) {
      const msg = err?.response?.data?.message || 'Có lỗi xảy ra khi tạo đặt phòng. Vui lòng thử lại.';
      showToast(msg, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      <style>{`
        /* Custom calendar styling to match Elysian dark/red theme */
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
      <div className="w-full min-h-screen pt-36 pb-24 bg-gray-50 flex items-start justify-center px-4 font-['Montserrat']">
        <div className="max-w-6xl w-full grid grid-cols-1 lg:grid-cols-12 gap-8 text-left">
          
          {/* Left Column: Wizard Steps (col-span-7) */}
          <div className="lg:col-span-7 bg-white p-8 md:p-10 border border-outline-variant shadow-lg flex flex-col justify-between">
            <div>
              {/* Back button */}
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

              {/* Wizard Progress Header */}
              <div className="flex justify-between items-center mb-8 border-b border-gray-100 pb-4">
                <div className="flex items-center gap-6">
                  {/* Step indicators */}
                  <div className="flex items-center gap-2">
                    <span className={`w-6 h-6 flex items-center justify-center text-[10px] font-black border ${currentStep >= 1 ? 'bg-primary border-primary text-white' : 'border-slate-300 text-slate-400'}`}>1</span>
                    <span className={`text-[10px] font-black uppercase tracking-wider hidden sm:inline ${currentStep === 1 ? 'text-primary' : 'text-slate-400'}`}>{t('booking_step1_label', 'Thông tin')}</span>
                  </div>
                  <span className="h-px w-6 bg-slate-350 hidden sm:inline" />
                  <div className="flex items-center gap-2">
                    <span className={`w-6 h-6 flex items-center justify-center text-[10px] font-black border ${currentStep >= 2 ? 'bg-primary border-primary text-white' : 'border-slate-300 text-slate-400'}`}>2</span>
                    <span className={`text-[10px] font-black uppercase tracking-wider hidden sm:inline ${currentStep === 2 ? 'text-primary' : 'text-slate-400'}`}>{t('booking_step2_label', 'Check-in')}</span>
                  </div>
                  <span className="h-px w-6 bg-slate-350 hidden sm:inline" />
                  <div className="flex items-center gap-2">
                    <span className={`w-6 h-6 flex items-center justify-center text-[10px] font-black border ${currentStep >= 3 ? 'bg-primary border-primary text-white' : 'border-slate-300 text-slate-400'}`}>3</span>
                    <span className={`text-[10px] font-black uppercase tracking-wider hidden sm:inline ${currentStep === 3 ? 'text-primary' : 'text-slate-400'}`}>{t('booking_step3_label', 'Xác nhận')}</span>
                  </div>
                </div>
                <div className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">{t('booking_step_progress_text', 'Bước')} {currentStep} / 3</div>
              </div>

              {/* WIZARD STEP 1: DATES, AVAILABILITY, GUESTS */}
              {currentStep === 1 && (
                <div className="space-y-6 animate-fade-in">
                  <h3 className="font-headline-lg text-lg text-primary uppercase italic tracking-wider m-0">{t('booking_step1_title', 'Bước 1: Chọn ngày & Số lượng khách')}</h3>
                  
                  {/* Room Type */}
                  <div className="space-y-2">
                    <label className="block text-xs font-bold text-secondary uppercase tracking-widest font-['Montserrat']">{t('booking_room_type_label', 'Loại Phòng')}</label>
                    <select 
                      value={selectedRoom ? selectedRoom.id : ''}
                      onChange={(e) => handleRoomChange(e.target.value)}
                      className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary"
                    >
                      {roomTypes.map((room) => (
                        <option key={room.id} value={room.id}>
                          {room.name} - {t('booking_capacity_label', 'Sức chứa')}: {room.totalCapacity || ((room.adultCapacity || 2) + (room.childCapacity || 1))} {t('booking_guests_count', 'khách')}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* React Datepicker Range */}
                  <div className="space-y-2">
                    <label className="block text-xs font-bold text-secondary uppercase tracking-widest">{t('booking_duration_label', 'Thời gian lưu trú (Check-in - Check-out)')}</label>
                    <div className="relative">
                      <DatePicker
                        selectsRange={true}
                        startDate={startDate}
                        endDate={endDate}
                        onChange={handleDateChange}
                        minDate={new Date()}
                        placeholderText={t('booking_dates_placeholder', 'Chọn khoảng ngày nhận và trả phòng')}
                        isClearable={true}
                        className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary"
                      />
                    </div>
                    {errors.dates && <p className="text-xs text-error font-medium mt-1">{errors.dates}</p>}
                  </div>

                  {/* Check Availability Box */}
                  {startDate && endDate && (
                    <div className="bg-slate-50 border border-slate-200 p-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                      <div>
                        <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">{t('booking_availability_status_label', 'Trạng thái phòng trống')}</span>
                        {isCheckingAvailability ? (
                          <span className="text-xs font-bold text-slate-600 flex items-center gap-1.5 mt-1">
                            <span className="w-3 h-3 border border-slate-400 border-t-slate-600 rounded-full animate-spin"></span> {t('booking_checking_availability', 'Đang kiểm tra phòng trống...')}
                          </span>
                        ) : isAvailable ? (
                          <span className="text-xs font-bold text-green-600 flex items-center gap-1 mt-1">
                            <span className="material-symbols-outlined text-base">check_circle</span> {t('booking_available_success', 'Phòng trống sẵn sàng')}
                          </span>
                        ) : isAvailable === false ? (
                          <span className="text-xs font-bold text-error flex items-center gap-1 mt-1">
                            <span className="material-symbols-outlined text-base">cancel</span> {t('booking_not_available', 'Hết phòng trống')}
                          </span>
                        ) : (
                          <span className="text-xs font-bold text-slate-500 mt-1 block">{t('booking_not_checked', 'Chưa kiểm tra')}</span>
                        )}
                      </div>
                      <button
                        type="button"
                        onClick={checkAvailability}
                        className="bg-slate-900 hover:bg-primary text-white text-[10px] font-black uppercase tracking-widest px-4 py-2 border-none cursor-pointer transition-all duration-200"
                      >
                        {t('booking_btn_check_availability', 'Kiểm tra phòng trống')}
                      </button>
                    </div>
                  )}

                  {/* Guests Steppers */}
                  <div className="space-y-4 border-t border-gray-100 pt-6">
                    <div className="flex justify-between items-center">
                      <label className="block text-xs font-bold text-secondary uppercase tracking-widest">{t('booking_guests_stepper_label', 'Số lượng khách nghỉ')}</label>
                      <span className="text-[10px] font-black text-primary uppercase tracking-wider bg-primary/5 px-2.5 py-1">{t('booking_max_capacity', 'Sức chứa tối đa')}: {maxCapacity} {t('booking_guests_count', 'khách')}</span>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                      {/* Adults Stepper */}
                      <div className="flex justify-between items-center border border-slate-200 p-3">
                        <div>
                          <span className="text-xs font-extrabold uppercase text-slate-700 block">{t('booking_adults_label', 'Người lớn')}</span>
                          <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">{t('booking_adults_sub', 'Trên 12 tuổi')}</span>
                        </div>
                        <div className="flex items-center gap-3">
                          <button
                            type="button"
                            onClick={() => setAdults(prev => Math.max(1, prev - 1))}
                            className="w-8 h-8 rounded-none border border-slate-300 bg-transparent text-slate-700 hover:bg-slate-100 font-black flex items-center justify-center cursor-pointer"
                          >
                            -
                          </button>
                          <span className="text-sm font-black w-4 text-center">{adults}</span>
                          <button
                            type="button"
                            onClick={() => {
                              if (totalGuests < maxCapacity) {
                                setAdults(prev => prev + 1);
                              } else {
                                showToast(`${t('booking_warning_capacity_prefix', 'Tổng số lượng khách không được vượt quá sức chứa')} ${maxCapacity} ${t('booking_warning_capacity_suffix', 'khách')}.`, 'warning');
                              }
                            }}
                            className="w-8 h-8 rounded-none border border-slate-300 bg-transparent text-slate-700 hover:bg-slate-100 font-black flex items-center justify-center cursor-pointer"
                          >
                            +
                          </button>
                        </div>
                      </div>

                      {/* Children Stepper */}
                      <div className="flex justify-between items-center border border-slate-200 p-3">
                        <div>
                          <span className="text-xs font-extrabold uppercase text-slate-700 block">{t('booking_children_label', 'Trẻ em')}</span>
                          <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">{t('booking_children_sub', 'Dưới 12 tuổi')}</span>
                        </div>
                        <div className="flex items-center gap-3">
                          <button
                            type="button"
                            onClick={() => setChildrenCount(prev => Math.max(0, prev - 1))}
                            className="w-8 h-8 rounded-none border border-slate-300 bg-transparent text-slate-700 hover:bg-slate-100 font-black flex items-center justify-center cursor-pointer"
                          >
                            -
                          </button>
                          <span className="text-sm font-black w-4 text-center">{childrenCount}</span>
                          <button
                            type="button"
                            onClick={() => {
                              if (totalGuests < maxCapacity) {
                                setChildrenCount(prev => prev + 1);
                              } else {
                                showToast(`${t('booking_warning_capacity_prefix', 'Tổng số lượng khách không được vượt quá sức chứa')} ${maxCapacity} ${t('booking_warning_capacity_suffix', 'khách')}.`, 'warning');
                              }
                            }}
                            className="w-8 h-8 rounded-none border border-slate-300 bg-transparent text-slate-700 hover:bg-slate-100 font-black flex items-center justify-center cursor-pointer"
                          >
                            +
                          </button>
                        </div>
                      </div>
                    </div>
                    {errors.guests && <p className="text-xs text-error font-medium mt-1">{errors.guests}</p>}
                  </div>

                  <button
                    type="button"
                    onClick={handleStep1Submit}
                    className="w-full bg-primary text-on-primary font-bold py-4 uppercase tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center h-12"
                  >
                    {t('booking_btn_continue_checkin', 'Tiếp tục chọn Check-in')}
                  </button>
                </div>
              )}

              {/* WIZARD STEP 2: CHECK-IN METHOD */}
              {currentStep === 2 && (
                <div className="space-y-6 animate-fade-in">
                  <h3 className="font-headline-lg text-lg text-primary uppercase italic tracking-wider m-0">{t('booking_step2_title', 'Bước 2: Chọn phương thức nhận phòng')}</h3>
                  <p className="text-secondary text-xs font-bold uppercase tracking-widest">{t('booking_step2_subtitle', 'Lựa chọn 1 trong các hình thức nhận phòng tại Elysian Smart Hotel')}</p>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    {/* QR Code Option */}
                    <div 
                      onClick={() => setCheckInMethod('QR Code')}
                      className={`border p-6 flex flex-col items-center justify-center text-center cursor-pointer transition-all duration-300 ${
                        checkInMethod === 'QR Code' 
                          ? 'border-primary bg-primary/5 text-primary shadow-md' 
                          : 'border-slate-200 hover:border-primary/50 text-slate-700 bg-white'
                      }`}
                    >
                      <span className="material-symbols-outlined text-3xl mb-3">qr_code_2</span>
                      <span className="text-[12px] font-black uppercase tracking-wider block">QR Code</span>
                      <span className="text-[9.5px] text-slate-500 font-bold uppercase tracking-wider mt-1.5">{t('booking_checkin_qr_sub', 'Check-in tự động')}</span>
                    </div>

                    {/* FaceID Option */}
                    <div 
                      onClick={() => setCheckInMethod('FaceID')}
                      className={`border p-6 flex flex-col items-center justify-center text-center cursor-pointer transition-all duration-300 ${
                        checkInMethod === 'FaceID' 
                          ? 'border-primary bg-primary/5 text-primary shadow-md' 
                          : 'border-slate-200 hover:border-primary/50 text-slate-700 bg-white'
                      }`}
                    >
                      <span className="material-symbols-outlined text-3xl mb-3">face</span>
                      <span className="text-[12px] font-black uppercase tracking-wider block">FaceID eKYC</span>
                      <span className="text-[9.5px] text-slate-500 font-bold uppercase tracking-wider mt-1.5">{t('booking_checkin_face_sub', 'Nhận diện khuôn mặt')}</span>
                    </div>

                    {/* Manual Option */}
                    <div 
                      onClick={() => setCheckInMethod('Manual')}
                      className={`border p-6 flex flex-col items-center justify-center text-center cursor-pointer transition-all duration-300 ${
                        checkInMethod === 'Manual' 
                          ? 'border-primary bg-primary/5 text-primary shadow-md' 
                          : 'border-slate-200 hover:border-primary/50 text-slate-700 bg-white'
                      }`}
                    >
                      <span className="material-symbols-outlined text-3xl mb-3">hotel_class</span>
                      <span className="text-[12px] font-black uppercase tracking-wider block">Manual</span>
                      <span className="text-[9.5px] text-slate-500 font-bold uppercase tracking-wider mt-1.5">{t('booking_checkin_manual_sub', 'Tại quầy lễ tân')}</span>
                    </div>
                  </div>

                  {/* Special Requests */}
                  <div className="space-y-2 border-t border-gray-100 pt-6">
                    <label className="block text-xs font-bold text-secondary uppercase tracking-widest">{t('booking_special_requests_label', 'Yêu Cầu Đặc Biệt (Tùy chọn)')}</label>
                    <textarea 
                      rows="3"
                      value={specialRequests}
                      onChange={(e) => setSpecialRequests(e.target.value)}
                      placeholder={t('booking_special_requests_placeholder', 'Ghi chú về giường phụ, phòng không hút thuốc, thời gian nhận phòng dự kiến...')}
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

              {/* WIZARD STEP 3: SUMMARY & TERMS & CONFIRM */}
              {currentStep === 3 && (
                <div className="space-y-6 animate-fade-in">
                  <h3 className="font-headline-lg text-lg text-primary uppercase italic tracking-wider m-0">{t('booking_step3_title', 'Bước 3: Tổng kết & Điều khoản')}</h3>
                  
                  {/* Terms & Conditions details box */}
                  <div className="bg-slate-50 border border-slate-200 p-5 space-y-3">
                    <span className="block text-xs font-black text-slate-900 uppercase tracking-wider border-b border-slate-200 pb-1.5">{t('booking_tc_title', 'Điều khoản & Điều kiện đặt phòng (T&C)')}</span>
                    <div className="text-[10px] text-slate-500 font-semibold uppercase tracking-wider leading-relaxed max-h-48 overflow-y-auto pr-2">
                      <p className="mb-2">{t('booking_tc_1', '1. THỦ TỤC NHẬN PHÒNG: Giờ nhận phòng tiêu chuẩn là từ 2h trưa (14:00). Khách hàng nhận phòng thông minh bằng mã QR hoặc khuôn mặt FaceID đã đăng ký.')}</p>
                      <p className="mb-2">{t('booking_tc_2', '2. THỦ TỤC TRẢ PHÒNG: Giờ trả phòng là trước 12h trưa (12:00). Việc trả phòng trễ sau 12:00 sẽ bị phụ thu phí tuỳ theo quy định khách sạn.')}</p>
                      <p className="mb-2">{t('booking_tc_3', '3. CHÍNH SÁCH HỦY: Hủy phòng miễn phí trước 24 giờ. Hủy phòng trễ hoặc không đến sẽ bị trừ tiền đặt cọc tương đương đêm đầu tiên.')}</p>
                      <p className="mb-2">{t('booking_tc_4', '4. SỨC CHỨA: Không vượt quá giới hạn số lượng khách đã chọn trong bước 1.')}</p>
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
                    <span className="text-xs font-bold text-slate-700 leading-tight">{t('booking_tc_agree_text', 'Tôi đã đọc, hiểu và đồng ý với toàn bộ Điều khoản & Điều kiện đặt phòng của Elysian Hotels.')}</span>
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
                        {t('booking_btn_processing', 'ĐANG XỬ LÝ...')}
                      </>
                    ) : t('booking_btn_confirm', 'XÁC NHẬN ĐẶT PHÒNG')}
                  </button>
                </div>
              )}
            </div>
          </div>

          {/* Right Column: Booking Summary - UPDATED IN REAL-TIME (col-span-5) */}
          <div className="lg:col-span-5 bg-white border border-outline-variant shadow-lg p-8 flex flex-col justify-between h-fit">
            <div>
              <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest mb-4 border-b border-gray-100 pb-2">
                {t('booking_summary_realtime', 'Tóm Tắt Chi Phí (Real-time)')}
              </h3>

              {selectedRoom && (
                <div className="space-y-4">
                  {/* Room Image */}
                  <div className="w-full h-48 overflow-hidden bg-slate-100 border border-slate-200">
                    <img 
                      src={selectedRoom.primaryImageUrl || 'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=1000&q=80'} 
                      alt={selectedRoom.name}
                      className="w-full h-full object-cover"
                    />
                  </div>

                  {/* Room Title */}
                  <div>
                    <span className="text-[10px] font-black text-primary uppercase tracking-widest block mb-0.5">
                      {selectedRoom.bedType || t('room_detail_premium_class', 'HẠNG PHÒNG THƯỢNG HẠNG')}
                    </span>
                    <h4 className="text-lg font-black text-slate-950 uppercase tracking-wider">
                      {selectedRoom.name}
                    </h4>
                  </div>

                  {/* Real-time Summary Details */}
                  <div className="space-y-2 border-t border-b border-gray-100 py-4 text-xs font-bold text-slate-700">
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('booking_summary_price_per_night', 'Giá mỗi đêm:')}</span>
                      <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(basePrice)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('booking_summary_duration', 'Thời gian:')}</span>
                      {startDate && endDate ? (
                        <span>{formatDateString(startDate)} {t('booking_summary_date_to', 'đến')} {formatDateString(endDate)} ({nights} {t('booking_summary_nights', 'đêm')})</span>
                      ) : (
                        <span className="text-slate-400 italic">{t('booking_summary_no_date', 'Chưa chọn ngày')}</span>
                      )}
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('booking_summary_guests', 'Số lượng khách:')}</span>
                      <span>{totalGuests} {t('booking_summary_people', 'người')} ({adults} {t('booking_summary_nl', 'NL')}, {childrenCount} {t('booking_summary_te', 'TE')})</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('booking_summary_checkin_method', 'Hình thức check-in:')}</span>
                      <span className="text-primary uppercase">{checkInMethod === 'Manual' ? t('booking_checkin_manual_option', 'Quầy lễ tân') : checkInMethod === 'FaceID' ? 'FaceID eKYC' : t('booking_checkin_qr_option', 'Mã QR')}</span>
                    </div>
                  </div>

                  {/* Price breakdown */}
                  <div className="space-y-2 pt-2 text-xs font-bold text-slate-700">
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('booking_summary_subtotal', 'Tạm tính (chưa thuế):')}</span>
                      <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(totalAmount)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('booking_summary_vat', 'Thuế VAT (10%):')}</span>
                      <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(vatAmount)}</span>
                    </div>
                    <div className="flex justify-between border-t border-slate-900 pt-3 text-sm">
                      <span className="font-black text-slate-900 uppercase tracking-wider">{t('booking_summary_total', 'TỔNG CỘNG:')}</span>
                      <span className="font-black text-primary text-base">
                        {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(finalAmount)}
                      </span>
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div className="mt-8 bg-slate-50 border border-slate-100 p-4 text-[9px] text-slate-500 font-semibold leading-relaxed uppercase tracking-wider">
              {t('booking_summary_system_notice', 'Hệ thống Smart Hotel tự động áp dụng thông tin check-in và tính giá chuẩn xác.')}
            </div>
          </div>
          
        </div>
      </div>
    </>
  );
}
