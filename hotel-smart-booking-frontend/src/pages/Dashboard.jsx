import DashboardSidebar from '../components/dashboard/DashboardSidebar';
import DashboardHeader from '../components/dashboard/DashboardHeader';
import HeroBanner from '../components/dashboard/HeroBanner';
import StatsGrid from '../components/dashboard/StatsGrid';
import CurrentBooking from '../components/dashboard/CurrentBooking';
import BookingHistory from '../components/dashboard/BookingHistory';
import ConciergeRequests from '../components/dashboard/ConciergeRequests';
import IdentityCard from '../components/dashboard/IdentityCard';
import PaymentOverview from '../components/dashboard/PaymentOverview';
import RecommendedSuites from '../components/dashboard/RecommendedSuites';
import UpdatesFeed from '../components/dashboard/UpdatesFeed';
import {
  USER_PROFILE,
  STATS,
  CURRENT_BOOKING,
  BOOKING_HISTORY,
  CONCIERGE_REQUESTS,
  UPDATES,
  PAYMENT_OVERVIEW,
  RECOMMENDED_SUITES,
} from '../data/dashboardData';

const toCurrentBooking = (booking) => ({
  ...booking,
  checkIn: { ...booking.checkIn, dayTime: booking.checkIn.detail },
  checkOut: { ...booking.checkOut, dayTime: booking.checkOut.detail },
  guests: { count: booking.guests.count, bedInfo: booking.guests.detail },
});

const toBookingHistory = (bookings) =>
  bookings.map((item) => ({
    ...item,
    name: item.destination,
  }));

export default function Dashboard({ setActivePage }) {
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
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <DashboardSidebar
        setActivePage={setActivePage}
        onNavigate={handleSidebarNavigate}
      />

      <div className="ml-72 min-h-screen">
        <DashboardHeader avatarUrl={USER_PROFILE.avatarUrl} />

        <div className="p-8 flex flex-col gap-8">
          <HeroBanner
            name={USER_PROFILE.name}
            tier={USER_PROFILE.tier}
            nextStayDate={USER_PROFILE.nextStayDate}
          />

          <StatsGrid stats={STATS} />

          <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">
            <div className="xl:col-span-2 flex flex-col gap-8">
              <CurrentBooking booking={toCurrentBooking(CURRENT_BOOKING)} />
              <BookingHistory bookings={toBookingHistory(BOOKING_HISTORY)} />
              <ConciergeRequests requests={CONCIERGE_REQUESTS} />
            </div>

            <div className="flex flex-col gap-8">
              <IdentityCard />
              <UpdatesFeed updates={UPDATES} />
              <PaymentOverview payment={PAYMENT_OVERVIEW} />
            </div>
          </div>

          <RecommendedSuites suites={RECOMMENDED_SUITES} />
        </div>
      </div>
    </div>
  );
}
