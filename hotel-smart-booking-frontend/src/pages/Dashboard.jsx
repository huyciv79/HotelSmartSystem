import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import DashboardSidebar from '../components/dashboard/DashboardSidebar';
import hotelLogo from '../assets/hotel_logo.png';
import HeroBanner from '../components/dashboard/HeroBanner';
import CurrentBooking from '../components/dashboard/CurrentBooking';
import BookingHistory from '../components/dashboard/BookingHistory';
import IdentityCard from '../components/dashboard/IdentityCard';
import EkycHub from './ekyc/EkycHub';
import Profile from './Profile';
import { getUserProfile } from '../services/userService';
import { getBookingHistory } from '../services/bookingService';
import { useToast, ToastContainer } from '../components/Toast';
import { useLanguage } from '../context/LanguageContext';
import {
  USER_PROFILE,
  CURRENT_BOOKING,
  BOOKING_HISTORY,
} from '../data/dashboardData';

const mapRealToCurrentBooking = (bk) => {
  if (!bk) return null;
  return {
    suiteName: bk.roomType,
    refCode: bk.bookingNumber || `BK-${bk.bookingId}`,
    status: bk.status === 'Cancelled' ? 'Đã Hủy' : bk.status === 'Checked-in' || bk.status === 'Checked In' ? 'Đã nhận phòng' : bk.status === 'Checked-out' || bk.status === 'Checked Out' || bk.status === 'Completed' ? 'Đã trả phòng' : bk.status === 'Paid' ? 'Đã thanh toán' : bk.status === 'Partially Paid' ? 'Đã cọc 30%' : 'Chờ thanh toán',
    checkIn: {
      date: bk.checkInDate,
      dayTime: 'Từ 2:00 PM (14:00)',
    },
    checkOut: {
      date: bk.checkOutDate,
      dayTime: 'Trước 12:00 PM (12:00)',
    },
    guests: {
      count: '01 Phòng',
      bedInfo: `${bk.nights} đêm`,
    },
    bookingId: bk.bookingId,
  };
};

const mapRealToBookingHistory = (bookings) =>
  bookings.map((bk) => ({
    id: bk.bookingId,
    name: bk.roomType,
    roomType: bk.bookingNumber || `BK-${bk.bookingId}`,
    period: `${bk.checkInDate} đến ${bk.checkOutDate} (${bk.nights} đêm)`,
    amount: new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(bk.totalAmount),
    status: bk.status,
  }));

export default function Dashboard({ setActivePage }) {
  const { t } = useLanguage();
  const location = useLocation();
  const [activeTab, setActiveTab] = useState(() => location.state?.tab ?? 'overview');
  const [profile, setProfile] = useState(USER_PROFILE);
  const [realBookings, setRealBookings] = useState([]);
  const { toasts, showToast, dismissToast } = useToast();

  const mapRealToCurrentBooking = (bk) => {
    if (!bk) return null;
    return {
      suiteName: bk.roomType,
      refCode: bk.bookingNumber || `BK-${bk.bookingId}`,
      status: bk.status === 'Cancelled' ? t('status_cancelled', 'Đã Hủy') : bk.status === 'Checked-in' || bk.status === 'Checked In' ? t('status_checked_in', 'Đã nhận phòng') : bk.status === 'Checked-out' || bk.status === 'Checked Out' || bk.status === 'Completed' ? t('status_checked_out', 'Đã trả phòng') : t('status_confirmed', 'Đã xác nhận'),
      checkIn: {
        date: bk.checkInDate,
        dayTime: t('db_booking_checkin_time_default', 'Từ 2:00 PM (14:00)'),
      },
      checkOut: {
        date: bk.checkOutDate,
        dayTime: t('db_booking_checkout_time_default', 'Trước 12:00 PM (12:00)'),
      },
      guests: {
        count: t('db_booking_room_count_1', '01 Phòng'),
        bedInfo: `${bk.nights} ${t('booking_summary_nights', 'đêm')}`,
      },
      bookingId: bk.bookingId,
    };
  };

  const mapRealToBookingHistory = (bookings) =>
    bookings.map((bk) => ({
      id: bk.bookingId,
      name: bk.roomType,
      roomType: bk.bookingNumber || `BK-${bk.bookingId}`,
      period: `${bk.checkInDate} ${t('booking_summary_date_to', 'đến')} ${bk.checkOutDate} (${bk.nights} ${t('booking_summary_nights', 'đêm')})`,
      amount: new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(bk.totalAmount),
      status: bk.status,
    }));

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const token = localStorage.getItem('accessToken');
        if (token) {
          const response = await getUserProfile();
          if (response && response.data) {
            setProfile(response.data);
          }
        }
      } catch (err) {
        console.error('Lỗi khi tải thông tin hồ sơ:', err);
      }
    };

    const fetchHistory = async () => {
      try {
        const token = localStorage.getItem('accessToken');
        if (token) {
          const response = await getBookingHistory();
          if (response && response.data) {
            const overridden = response.data.map(bk => {
              const localStatus = localStorage.getItem(`booking_status_${bk.bookingId}`);
              return localStatus ? { ...bk, status: localStatus } : bk;
            });
            setRealBookings(overridden);
          }
        }
      } catch (err) {
        console.error('Lỗi khi tải lịch sử đặt phòng:', err);
      }
    };

    fetchProfile();
    fetchHistory();
  }, []);

  const handleSidebarNavigate = (key) => {
    if (key === 'logout') {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      setActivePage('home');
      return;
    }
    if (key === 'rewards') {
      setActivePage('rewards');
      return;
    }
    setActiveTab(key);
  };

  const handleViewBookingDetail = (bookingId) => {
    sessionStorage.setItem('selectedBookingId', bookingId);
    setActivePage('booking-detail');
  };

  const currentName = profile.fullName || profile.name || USER_PROFILE.name;
  const currentTier = profile.role === 'customer' ? 'MEMBER' : (profile.role || profile.tier || 'MEMBER').toUpperCase();

  const activeBookings = realBookings.filter(
    (bk) => bk.status !== 'Cancelled' && bk.status !== 'Checked-out' && bk.status !== 'Checked Out' && bk.status !== 'Completed'
  );
  const currentBooking = activeBookings.length > 0 ? activeBookings[0] : null;

  const nextStayDate = currentBooking
    ? new Date(currentBooking.checkInDate).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
    : t('db_stays_none', 'Chưa có lịch trình');

  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col md:flex-row text-slate-800 dashboard-font-semibold overflow-x-hidden w-full">
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />

      {/* Mobile Header Bar */}
      <div className="md:hidden bg-[#141416] text-white p-4 flex items-center justify-between sticky top-0 z-30 border-b border-neutral-800 shadow-md">
        <div 
          onClick={() => setActivePage('home')}
          className="cursor-pointer"
        >
          <h1 className="font-black text-lg tracking-[0.2em] uppercase m-0 leading-none text-white">
            ELYSIAN
          </h1>
          <span className="text-[6.5px] tracking-[0.3em] text-primary font-black uppercase leading-none mt-1 block">HOTELS & RESORTS</span>
        </div>

        <button
          onClick={() => setIsSidebarOpen(!isSidebarOpen)}
          className="p-2 rounded-lg bg-neutral-800 text-white hover:bg-neutral-700 active:scale-95 transition-all cursor-pointer border-none flex items-center justify-center"
          aria-label="Toggle Menu"
        >
          <span className="material-symbols-outlined text-2xl">
            {isSidebarOpen ? 'close' : 'menu'}
          </span>
        </button>
      </div>

      <DashboardSidebar
        activeItem={activeTab}
        setActivePage={setActivePage}
        onNavigate={handleSidebarNavigate}
        isOpen={isSidebarOpen}
        onClose={() => setIsSidebarOpen(false)}
      />

      <div className="ml-0 md:ml-72 flex-1 min-h-screen flex flex-col w-full max-w-full overflow-x-hidden">

        <div className="p-4 md:p-8 flex flex-col gap-6 md:gap-8 flex-1 w-full max-w-full">
          {activeTab === 'settings' ? (
            <Profile
              initialProfile={profile}
              onProfileUpdate={(updated) => {
                setProfile(updated);
                localStorage.setItem('user', JSON.stringify(updated));
              }}
              showToast={showToast}
            />
          ) : activeTab === 'stays' ? (
            <div className="bg-white border border-slate-200/80 rounded-2xl shadow-sm p-4 sm:p-6 md:p-8 flex flex-col font-['Montserrat'] w-full max-w-full">
              <div className="flex items-center gap-3 mb-2">
                <div className="w-10 h-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center shrink-0">
                  <span className="material-symbols-outlined text-2xl">hotel</span>
                </div>
                <div>
                  <h2 className="font-headline-lg text-lg sm:text-2xl font-black text-slate-900 uppercase tracking-wider m-0 leading-tight">
                    {t('db_stays_title', 'LỊCH SỬ ĐẶT PHÒNG CỦA TÔI')}
                  </h2>
                  <p className="text-slate-500 text-[10px] sm:text-xs font-semibold uppercase tracking-widest m-0 mt-0.5">
                    {t('db_stays_subtitle', 'Danh sách các phòng nghỉ bạn đã đăng ký lưu trú tại Elysian')}
                  </p>
                </div>
              </div>

              <div className="h-px bg-slate-100 w-full my-4 sm:my-6" />

              {realBookings.length === 0 ? (
                <div className="text-center py-16 bg-slate-50/50 rounded-xl border border-dashed border-slate-200">
                  <span className="material-symbols-outlined text-5xl text-slate-300 mb-2 block">hotel</span>
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-widest">{t('db_stays_empty', 'Bạn chưa có phòng nào được đặt.')}</p>
                </div>
              ) : (
                <div className="flex flex-col gap-4 w-full">
                  {realBookings.map((bk) => (
                    <div 
                      key={bk.bookingId} 
                      className="bg-slate-50/60 hover:bg-white border border-slate-200/80 hover:border-primary/40 rounded-xl p-4 sm:p-5 transition-all duration-300 shadow-xs hover:shadow-md flex flex-col gap-4 w-full"
                    >
                      {/* Top Header: Booking Code & Status Badge */}
                      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-200/60 pb-3">
                        <div className="flex items-center gap-2">
                          <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">{t('booking_ref_code_label', 'Mã đặt phòng')}:</span>
                          <span className="text-xs sm:text-sm font-black text-slate-900 uppercase tracking-widest bg-white px-2.5 py-1 rounded-md border border-slate-200 shadow-2xs">
                            {bk.bookingNumber || bk.bookingReference || `BK-${bk.bookingId}`}
                          </span>
                        </div>

                        <span className={`px-3 py-1 text-[9.5px] font-extrabold uppercase tracking-wider rounded-full shadow-2xs whitespace-nowrap ${
                          bk.status === 'Cancelled'
                            ? 'bg-rose-100 text-rose-700 border border-rose-200'
                            : bk.status === 'Checked-in' || bk.status === 'Checked In'
                              ? 'bg-sky-100 text-sky-700 border border-sky-200'
                              : bk.status === 'Checked-out' || bk.status === 'Checked Out' || bk.status === 'Completed'
                                ? 'bg-slate-200 text-slate-700 border border-slate-300'
                                : bk.status === 'Paid'
                                  ? 'bg-emerald-100 text-emerald-700 border border-emerald-200'
                                  : bk.status === 'Partially Paid'
                                    ? 'bg-indigo-100 text-indigo-700 border border-indigo-200'
                                    : 'bg-amber-100 text-amber-700 border border-amber-200'
                        }`}>
                          {bk.status === 'Cancelled'
                            ? 'Đã Hủy'
                            : bk.status === 'Checked-in' || bk.status === 'Checked In'
                              ? 'Đã nhận phòng'
                              : bk.status === 'Checked-out' || bk.status === 'Checked Out' || bk.status === 'Completed'
                                ? 'Đã trả phòng'
                                : bk.status === 'Paid'
                                  ? 'Đã thanh toán'
                                  : bk.status === 'Partially Paid'
                                    ? 'Đã cọc 30%'
                                    : 'Chờ thanh toán'}
                        </span>
                      </div>

                      {/* Middle Content: Room Type & Stay Duration */}
                      <div className="flex flex-col gap-1">
                        <h4 className="text-sm sm:text-base font-black text-slate-900 uppercase tracking-wide m-0">
                          {bk.roomType}
                        </h4>
                        <p className="text-xs text-slate-500 font-semibold m-0 flex items-center gap-1.5 flex-wrap">
                          <span className="material-symbols-outlined text-base text-primary/80">calendar_month</span>
                          <span>{t('booking_summary_duration', 'Thời gian')}</span>
                          <span className="font-bold text-slate-700">{bk.checkInDate}</span>
                          <span>{t('booking_summary_date_to', 'đến')}</span>
                          <span className="font-bold text-slate-700">{bk.checkOutDate}</span>
                          <span className="text-slate-400 font-normal">({bk.nights} {t('booking_summary_nights', 'đêm')})</span>
                        </p>
                      </div>

                      {/* Bottom Footer: Price & Action Button */}
                      <div className="flex flex-wrap sm:flex-nowrap items-center justify-between gap-3 pt-3 border-t border-slate-200/60">
                        <div>
                          <span className="text-[9.5px] text-slate-400 font-bold uppercase tracking-wider block leading-none mb-1">
                            {t('db_total_cost_label', 'Tổng chi phí')}
                          </span>
                          <span className="text-sm sm:text-base font-black text-primary tracking-tight">
                            {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(bk.totalAmount)}
                          </span>
                        </div>

                        <button
                          onClick={() => handleViewBookingDetail(bk.bookingId)}
                          className="w-full sm:w-auto px-5 py-2.5 bg-primary hover:bg-slate-900 active:scale-97 text-white font-bold uppercase text-[10.5px] tracking-wider rounded-xl transition-all duration-200 cursor-pointer border-none shadow-xs hover:shadow-md flex items-center justify-center gap-1.5"
                        >
                          <span>{t('db_btn_view_detail', 'Xem Chi Tiết')}</span>
                          <span className="material-symbols-outlined text-base">arrow_forward</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ) : activeTab === 'ekyc' ? (
            <EkycHub onBack={() => setActiveTab('overview')} />
          ) : (
            <>
              <HeroBanner
                name={currentName}
                tier={currentTier}
                nextStayDate={nextStayDate}
              />

              <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2 flex flex-col gap-8">
                  {currentBooking ? (
                    <CurrentBooking
                      booking={mapRealToCurrentBooking(currentBooking)}
                      onViewDetail={() => handleViewBookingDetail(currentBooking.bookingId)}
                    />
                  ) : (
                    <div className="bg-white border border-outline-variant shadow-lg p-8 text-center font-['Montserrat'] flex flex-col items-center justify-center min-h-[220px]">
                      <span className="material-symbols-outlined text-4xl text-slate-300 mb-3">hotel</span>
                      <h3 className="text-xs font-black text-slate-900 uppercase tracking-widest mb-2">{t('db_no_upcoming_title', 'Bạn không có đặt phòng nào sắp tới')}</h3>
                      <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider mb-5 max-w-md leading-relaxed">{t('db_no_upcoming_desc', 'Hãy khám phá các ưu đãi và đặt phòng nghỉ sang trọng tại Elysian ngay hôm nay.')}</p>
                      <button
                        onClick={() => setActivePage('home')}
                        className="bg-primary hover:bg-slate-950 text-white font-bold py-3 px-8 uppercase text-[10px] tracking-widest transition-all cursor-pointer border-none parallelogram-btn h-10"
                      >
                        {t('db_btn_discover_now', 'Khám phá ngay')}
                      </button>
                    </div>
                  )}
                </div>

                <div className="lg:col-span-1 flex flex-col gap-8">
                  <IdentityCard onNavigate={() => setActiveTab('ekyc')} />
                  <BookingHistory
                    bookings={mapRealToBookingHistory(realBookings.slice(0, 3))}
                    onViewDetail={(id) => handleViewBookingDetail(id)}
                    onViewAll={() => setActiveTab('stays')}
                  />
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
