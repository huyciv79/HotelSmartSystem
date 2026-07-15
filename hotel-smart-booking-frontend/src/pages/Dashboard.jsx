import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import DashboardSidebar from '../components/dashboard/DashboardSidebar';
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

  return (
    <div className="min-h-screen bg-gray-50 flex text-slate-800 dashboard-font-semibold">
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />

      <DashboardSidebar
        activeItem={activeTab}
        setActivePage={setActivePage}
        onNavigate={handleSidebarNavigate}
      />

      <div className="ml-72 flex-1 min-h-screen flex flex-col">

        <div className="p-8 flex flex-col gap-8 flex-1">
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
            <div className="bg-white border border-outline-variant shadow-lg p-8 md:p-10 flex flex-col font-['Montserrat']">
              <h2 className="font-headline-lg text-headline-md text-primary uppercase italic tracking-wider m-0 mb-1">
                {t('db_stays_title', 'LỊCH SỬ ĐẶT PHÒNG CỦA TÔI')}
              </h2>
              <p className="text-secondary text-[10px] font-bold uppercase tracking-widest border-b border-gray-100 pb-4 mb-6">
                {t('db_stays_subtitle', 'Danh sách các phòng nghỉ bạn đã đăng ký lưu trú tại Elysian')}
              </p>

              {realBookings.length === 0 ? (
                <div className="text-center py-16">
                  <span className="material-symbols-outlined text-4xl text-slate-300 mb-2">hotel</span>
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-widest">{t('db_stays_empty', 'Bạn chưa có phòng nào được đặt.')}</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {realBookings.map((bk) => (
                    <div key={bk.bookingId} className="border border-slate-200 p-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-6 hover:border-primary transition-all duration-150 bg-white">
                      <div>
                        <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">{t('booking_ref_code_label', 'Mã đặt phòng')}</span>
                        <span className="text-sm font-black text-slate-900 uppercase tracking-widest">{bk.bookingNumber || bk.bookingReference || `BK-${bk.bookingId}`}</span>
                        <h4 className="text-sm font-black text-slate-800 uppercase tracking-wider mt-1.5">{bk.roomType}</h4>
                        <p className="text-xs text-slate-500 font-bold mt-1 uppercase tracking-wide">{t('booking_summary_duration', 'Thời gian')}: {bk.checkInDate} {t('booking_summary_date_to', 'đến')} {bk.checkOutDate} ({bk.nights} {t('booking_summary_nights', 'đêm')})</p>
                      </div>

                      <div className="flex items-center gap-6 self-stretch md:self-auto justify-between md:justify-end">
                        <div className="text-right">
                          <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">{t('db_total_cost_label', 'Tổng chi phí')}</span>
                          <span className="text-xs font-black text-primary">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(bk.totalAmount)}</span>
                        </div>
                        <div className="text-right">
                          <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">{t('db_status_label', 'Trạng thái')}</span>
                          <span className={`px-3 py-1.5 text-[9px] font-black uppercase tracking-widest ${bk.status === 'Cancelled'
                              ? 'bg-red-100 text-red-700'
                              : bk.status === 'Checked-in' || bk.status === 'Checked In'
                                ? 'bg-blue-100 text-blue-700'
                                : bk.status === 'Checked-out' || bk.status === 'Checked Out' || bk.status === 'Completed'
                                  ? 'bg-slate-200 text-slate-700'
                                  : bk.status === 'Paid'
                                    ? 'bg-green-100 text-green-700'
                                    : bk.status === 'Partially Paid'
                                      ? 'bg-indigo-100 text-indigo-700'
                                      : 'bg-yellow-100 text-yellow-700'
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
                        <button
                          onClick={() => handleViewBookingDetail(bk.bookingId)}
                          className="bg-primary hover:bg-slate-900 text-white font-bold py-3 px-8 uppercase text-[10px] tracking-widest transition-all cursor-pointer border-none flex items-center justify-center h-10 parallelogram-btn"
                        >
                          {t('db_btn_view_detail', 'Xem Chi Tiết')}
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
