import { useState } from 'react';
import { Calendar, Percent, Key, DollarSign, UserCheck, Shield, ChevronLeft, ChevronRight } from 'lucide-react';

const StaffOverview = ({
  bookings,
  roomTypes,
  currentUser,
  isManager,
  setActiveTab,
  handleOpenAddRoom,
  setSelectedBooking,
  startScanner,
  handleDirectCheckInOut,
  handleOpenWalkIn,
  stats
}) => {
  const [carouselIndex, setCarouselIndex] = useState(0);

  // Calcs
  const confirmedCount = bookings.filter(b => ['confirmed', 'paid', 'partially paid'].includes(String(b.status || '').toLowerCase())).length;
  const checkedInCount = bookings.filter(b => ['checked in', 'checked-in', 'staying'].includes(String(b.status || '').toLowerCase())).length;

  const displayConfirmed = stats ? stats.confirmedCount : confirmedCount;
  const displayCheckedIn = stats ? stats.checkedInCount : checkedInCount;
  const displayOccupancyRate = stats ? `${stats.occupancyRate.toFixed(1)}%` : '0.0%';

  const formatExpectedRevenue = (amount) => {
    if (!amount) return '0 đ';
    if (amount >= 1000000) {
      return (amount / 1000000).toFixed(1) + 'M đ';
    }
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
  };
  
  const displayExpectedRevenue = stats ? formatExpectedRevenue(stats.expectedRevenue) : '0 đ';

  // Calculate real metrics from bookings
  const totalRealRevenue = bookings.reduce((sum, b) => {
    const status = String(b.status || '').toLowerCase();
    if (status !== 'cancelled') {
      return sum + (Number(b.totalAmount) || 0);
    }
    return sum;
  }, 0);

  // Segments breakdown
  const onlineBookingsCount = bookings.filter(b => {
    const type = String(b.bookingType || '').toLowerCase();
    return type === 'online' || (!type.includes('group') && !type.includes('walk'));
  }).length;
  const groupBookingsCount = bookings.filter(b => String(b.bookingType || '').toLowerCase().includes('group')).length;
  const walkInBookingsCount = bookings.filter(b => String(b.bookingType || '').toLowerCase().includes('walk')).length;
  const totalSegmentsCount = bookings.length || 1;
  const onlinePercent = ((onlineBookingsCount / totalSegmentsCount) * 100).toFixed(1);
  const groupPercent = ((groupBookingsCount / totalSegmentsCount) * 100).toFixed(1);
  const walkInPercent = ((walkInBookingsCount / totalSegmentsCount) * 100).toFixed(1);

  // Weekly Check-ins activity count
  const checkInDaysCount = { CN: 0, T2: 0, T3: 0, T4: 0, T5: 0, T6: 0, T7: 0 };
  bookings.forEach(b => {
    if (b.checkInDate) {
      try {
        const date = new Date(b.checkInDate);
        const day = date.getDay(); // 0 is CN, 1 is T2...
        const dayKeys = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
        checkInDaysCount[dayKeys[day]] += 1;
      } catch (e) {}
    }
  });
  const maxDayCount = Math.max(...Object.values(checkInDaysCount), 1);
  const todayDayIndex = new Date().getDay();
  const weeklyActivity = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'].map((day, idx) => ({
    day,
    val: Math.round((checkInDaysCount[day] / maxDayCount) * 90) || 10,
    count: checkInDaysCount[day],
    active: idx === todayDayIndex
  }));

  // Repeat Guest Rate calculation
  const guestEmails = bookings.map(b => String(b.email || '').trim().toLowerCase()).filter(Boolean);
  const guestCounts = {};
  guestEmails.forEach(email => {
    guestCounts[email] = (guestCounts[email] || 0) + 1;
  });
  const uniqueGuestsCount = Object.keys(guestCounts).length;
  const repeatGuestsCount = Object.values(guestCounts).filter(count => count > 1).length;
  const repeatRate = uniqueGuestsCount > 0 ? Math.round((repeatGuestsCount / uniqueGuestsCount) * 100) : 0;

  // Revenue trend grouping into 5 periods of current month
  const periods = [0, 0, 0, 0, 0];
  bookings.forEach(b => {
    if (b.checkInDate && String(b.status || '').toLowerCase() !== 'cancelled') {
      try {
        const day = new Date(b.checkInDate).getDate(); // 1 - 31
        const amount = Number(b.totalAmount) || 0;
        if (day <= 6) periods[0] += amount;
        else if (day <= 12) periods[1] += amount;
        else if (day <= 18) periods[2] += amount;
        else if (day <= 24) periods[3] += amount;
        else periods[4] += amount;
      } catch (e) {}
    }
  });
  const maxPeriodAmount = Math.max(...periods, 1);
  const chartY = periods.map(val => Math.round(100 - (val / maxPeriodAmount) * 75));
  const areaD = `M 10 ${chartY[0]} L 125 ${chartY[1]} L 250 ${chartY[2]} L 375 ${chartY[3]} L 490 ${chartY[4]} L 490 120 L 10 120 Z`;
  const lineD = `M 10 ${chartY[0]} L 125 ${chartY[1]} L 250 ${chartY[2]} L 375 ${chartY[3]} L 490 ${chartY[4]}`;

  // Filter active room types only
  const activeRoomTypes = (roomTypes || []).filter(room => {
    const status = String(room.status || '').toLowerCase().trim();
    return status !== 'inactive' && status !== 'deleted' && status !== 'disabled';
  });

  // Curated Room Types carousel
  const nextCarousel = () => {
    if (activeRoomTypes.length <= 3) return;
    setCarouselIndex((prev) => (prev + 1) % activeRoomTypes.length);
  };
  const prevCarousel = () => {
    if (activeRoomTypes.length <= 3) return;
    setCarouselIndex((prev) => (prev - 1 + activeRoomTypes.length) % activeRoomTypes.length);
  };


  return (
    <div className="space-y-8 animate-scale-in text-left">
      {/* Overview Top Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-slate-100 pb-5">
        <div>
          <span className="text-[10px] text-slate-400 font-extrabold uppercase tracking-widest block">THE IRIS HUB</span>
          <h1 className="text-2xl font-black text-slate-800 uppercase tracking-wide mt-1">Tổng Quan Vận Hành</h1>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200/60 rounded-xl shadow-sm text-xs font-semibold text-slate-650">
            <Calendar size={14} className="text-slate-400" />
            <span>{new Date().toLocaleDateString('vi-VN')}</span>
          </div>
          <button 
            onClick={() => setActiveTab('reports')}
            className="px-4 py-2 bg-primary hover:brightness-110 text-white border-none rounded-xl shadow-[0_4px_12px_rgba(162,5,19,0.2)] text-xs font-bold uppercase tracking-wider transition-all cursor-pointer"
          >
            Báo cáo
          </button>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Card 1: Check-in Requests */}
        <div className="bg-white border border-slate-100 rounded-2xl p-5 shadow-[0_8px_30px_rgb(0,0,0,0.015)] flex flex-col justify-between h-32 hover:scale-[1.01] transition-all">
          <div className="flex justify-between items-center">
            <span className="text-[11px] text-slate-400 font-extrabold uppercase tracking-wider">Chờ Check-in</span>
            <div className="w-7 h-7 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
              <span className="material-symbols-outlined text-sm font-bold">login</span>
            </div>
          </div>
          <div className="flex items-baseline gap-2 mt-3">
            <span className="text-2xl font-black text-slate-800 leading-none">{displayConfirmed}</span>
            <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] font-bold text-emerald-600 bg-emerald-50 rounded-md">
              ▲ 15.5%
            </span>
          </div>
          <span className="text-[10px] text-slate-400 mt-2 font-medium">so với kì trước</span>
        </div>

        {/* Card 2: Occupied Rooms */}
        <div className="bg-white border border-slate-100 rounded-2xl p-5 shadow-[0_8px_30px_rgb(0,0,0,0.015)] flex flex-col justify-between h-32 hover:scale-[1.01] transition-all">
          <div className="flex justify-between items-center">
            <span className="text-[11px] text-slate-400 font-extrabold uppercase tracking-wider">Đang lưu trú</span>
            <div className="w-7 h-7 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <span className="material-symbols-outlined text-sm font-bold">bed</span>
            </div>
          </div>
          <div className="flex items-baseline gap-2 mt-3">
            <span className="text-2xl font-black text-slate-800 leading-none">{displayCheckedIn}</span>
            <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] font-bold text-emerald-600 bg-emerald-50 rounded-md">
              ▲ 8.4%
            </span>
          </div>
          <span className="text-[10px] text-slate-400 mt-2 font-medium">so với kì trước</span>
        </div>

        {/* Card 3: Occupancy Rate */}
        <div className="bg-white border border-slate-100 rounded-2xl p-5 shadow-[0_8px_30px_rgb(0,0,0,0.015)] flex flex-col justify-between h-32 hover:scale-[1.01] transition-all">
          <div className="flex justify-between items-center">
            <span className="text-[11px] text-slate-400 font-extrabold uppercase tracking-wider">Tỷ lệ lấp đầy</span>
            <div className="w-7 h-7 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center">
              <span className="material-symbols-outlined text-sm font-bold">percent</span>
            </div>
          </div>
          <div className="flex items-baseline gap-2 mt-3">
            <span className="text-2xl font-black text-slate-800 leading-none">{displayOccupancyRate}</span>
            <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] font-bold text-rose-600 bg-rose-50 rounded-md">
              ▼ 1.2%
            </span>
          </div>
          <span className="text-[10px] text-slate-400 mt-2 font-medium">so với kì trước</span>
        </div>

        {/* Card 4: Expected Revenue */}
        <div className="bg-white border border-slate-100 rounded-2xl p-5 shadow-[0_8px_30px_rgb(0,0,0,0.015)] flex flex-col justify-between h-32 hover:scale-[1.01] transition-all">
          <div className="flex justify-between items-center">
            <span className="text-[11px] text-slate-400 font-extrabold uppercase tracking-wider">Doanh thu dự tính</span>
            <div className="w-7 h-7 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
              <span className="material-symbols-outlined text-sm font-bold">payments</span>
            </div>
          </div>
          <div className="flex items-baseline gap-2 mt-3">
            <span className="text-2xl font-black text-primary leading-none">{displayExpectedRevenue}</span>
            <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] font-bold text-emerald-600 bg-emerald-50 rounded-md">
              ▲ 4.4%
            </span>
          </div>
          <span className="text-[10px] text-slate-400 mt-2 font-medium">so với kì trước</span>
        </div>
      </div>

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Card: Total Profit (Doanh Thu & Đặt Phòng) */}
        <div className="lg:col-span-2 bg-white border border-slate-100 rounded-2xl p-6 shadow-[0_8px_30px_rgb(0,0,0,0.015)] flex flex-col justify-between">
          <div>
            <div className="flex justify-between items-center">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-wider">Doanh Thu Theo Kỳ</h3>
            </div>
            
            <div className="flex items-baseline gap-3 mt-4">
              <span className="text-3xl font-black text-slate-850">
                {formatExpectedRevenue(totalRealRevenue)}
              </span>
              <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] font-bold text-emerald-600 bg-emerald-50 rounded-md">
                ▲ 24.4%
              </span>
              <span className="text-[10px] text-slate-400 font-medium">so với kỳ trước</span>
            </div>
          </div>

          {/* SVG Line Chart */}
          <div className="w-full h-40 mt-6 relative">
            <svg viewBox="0 0 500 120" className="w-full h-full overflow-visible">
              <defs>
                <linearGradient id="chartGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#a20513" stopOpacity="0.12"/>
                  <stop offset="100%" stopColor="#a20513" stopOpacity="0.0"/>
                </linearGradient>
              </defs>
              
              {/* Grid Lines */}
              <line x1="0" y1="30" x2="500" y2="30" stroke="#f1f5f9" strokeWidth="1" strokeDasharray="3,3" />
              <line x1="0" y1="60" x2="500" y2="60" stroke="#f1f5f9" strokeWidth="1" strokeDasharray="3,3" />
              <line x1="0" y1="90" x2="500" y2="90" stroke="#f1f5f9" strokeWidth="1" strokeDasharray="3,3" />
              
              {/* Area under the line */}
              <path 
                d={areaD} 
                fill="url(#chartGradient)" 
              />
              
              {/* Curve Line */}
              <path 
                d={lineD} 
                fill="none" 
                stroke="#a20513" 
                strokeWidth="3.5" 
                strokeLinecap="round"
              />
              
              {/* Interactive Dots */}
              <circle cx="10" cy={chartY[0]} r="4.5" fill="#a20513" stroke="#fff" strokeWidth="2" className="shadow-md" />
              <circle cx="125" cy={chartY[1]} r="4.5" fill="#a20513" stroke="#fff" strokeWidth="2" className="shadow-md" />
              <circle cx="250" cy={chartY[2]} r="4.5" fill="#a20513" stroke="#fff" strokeWidth="2" className="shadow-md" />
              <circle cx="375" cy={chartY[3]} r="4.5" fill="#a20513" stroke="#fff" strokeWidth="2" className="shadow-md" />
              <circle cx="490" cy={chartY[4]} r="4.5" fill="#a20513" stroke="#fff" strokeWidth="2" className="shadow-md" />
            </svg>
            <div className="flex justify-between text-[9px] text-slate-400 font-extrabold uppercase mt-2 px-1 tracking-wider">
              <span>1 tháng 7</span>
              <span>8 tháng 7</span>
              <span>15 tháng 7</span>
              <span>22 tháng 7</span>
              <span>29 tháng 7</span>
            </div>
          </div>

          {/* Booking Segments Breakdown */}
          <div className="grid grid-cols-3 gap-4 mt-6">
            <div className="p-3 bg-slate-50/60 border border-slate-100 rounded-xl border-l-4 border-l-blue-500 text-left">
              <span className="text-[10px] text-slate-400 font-bold block uppercase tracking-wider">Đặt trực tuyến</span>
              <strong className="text-slate-700 text-sm mt-1 block">{onlinePercent}%</strong>
            </div>
            <div className="p-3 bg-slate-50/60 border border-slate-100 rounded-xl border-l-4 border-l-emerald-500 text-left">
              <span className="text-[10px] text-slate-400 font-bold block uppercase tracking-wider">Khách đoàn</span>
              <strong className="text-slate-700 text-sm mt-1 block">{groupPercent}%</strong>
            </div>
            <div className="p-3 bg-slate-50/60 border border-slate-100 rounded-xl border-l-4 border-l-amber-500 text-left">
              <span className="text-[10px] text-slate-400 font-bold block uppercase tracking-wider">Walk-in</span>
              <strong className="text-slate-700 text-sm mt-1 block">{walkInPercent}%</strong>
            </div>
          </div>
        </div>

        {/* Right Columns: Most Day Active & Repeat Customer Rate */}
        <div className="flex flex-col gap-6 h-full">
          {/* Card: Weekly Activity (Check-ins trong tuần) */}
          <div className="bg-white border border-slate-100 rounded-2xl p-6 shadow-[0_8px_30px_rgb(0,0,0,0.015)] flex flex-col justify-between flex-1">
            <div className="flex justify-between items-center">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-wider">Lượt Check-in</h3>
            </div>
            
            <div className="h-28 flex items-end justify-between gap-2 mt-4 px-1">
              {weeklyActivity.map((item, idx) => (
                <div key={idx} className="flex-1 flex flex-col items-center gap-2">
                  <div className="w-full bg-slate-50 rounded-full h-20 relative overflow-hidden">
                    <div 
                      className={`absolute bottom-0 left-0 right-0 rounded-full transition-all duration-500 ${
                        item.active ? 'bg-primary shadow-[0_4px_12px_rgba(162,5,19,0.3)]' : 'bg-slate-200 hover:bg-slate-300'
                      }`}
                      style={{ height: `${item.val}%` }}
                    />
                  </div>
                  <span className={`text-[10px] font-bold uppercase tracking-wider ${item.active ? 'text-primary' : 'text-slate-400'}`}>
                    {item.day}
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* Card: Repeat Customer Rate */}
          <div className="bg-white border border-slate-100 rounded-2xl p-6 shadow-[0_8px_30px_rgb(0,0,0,0.015)] flex flex-col justify-between flex-1">
            <div className="flex justify-between items-center">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-wider">Khách Hàng Quay Lại</h3>
            </div>
            
            <div className="flex flex-col items-center mt-3">
              <div className="relative w-36 h-20 flex items-center justify-center overflow-hidden">
                <svg className="w-full h-full" viewBox="0 0 100 50">
                  {/* Background Arc */}
                  <path 
                    d="M 10 50 A 40 40 0 0 1 90 50" 
                    fill="none" 
                    stroke="#f1f5f9" 
                    strokeWidth="8" 
                    strokeLinecap="round"
                  />
                  {/* Active Progress Arc */}
                  <path 
                    d="M 10 50 A 40 40 0 0 1 90 50" 
                    fill="none" 
                    stroke="#10b981" 
                    strokeWidth="8" 
                    strokeLinecap="round"
                    strokeDasharray="126"
                    strokeDashoffset={126 - (126 * repeatRate) / 100}
                    className="transition-all duration-1000 ease-out"
                  />
                </svg>
                <div className="absolute bottom-0 text-center">
                  <span className="text-2xl font-black text-slate-800 leading-none">{repeatRate}%</span>
                  <p className="text-[9px] text-slate-400 font-extrabold uppercase mt-1">Đạt chỉ tiêu 80%</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main Two-Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column (Recent Bookings / Guest List) */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white border border-slate-100 shadow-[0_8px_30px_rgb(0,0,0,0.015)] p-6 rounded-2xl">
            <div className="flex justify-between items-center border-b border-slate-100 pb-4 mb-4">
              <div>
                <h4 className="text-xs font-black text-slate-800 uppercase tracking-widest">Danh sách check-in gần đây</h4>
                <p className="text-[9px] text-slate-400 uppercase tracking-widest mt-1">Quản lý trực tiếp các đặt phòng đang diễn ra</p>
              </div>
              <button
                onClick={() => setActiveTab('operations')}
                className="text-primary hover:underline font-black text-[10px] uppercase tracking-wider border-none bg-transparent cursor-pointer"
              >
                Xử lý ngay
              </button>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full border-collapse text-left text-xs text-slate-650">
                <thead>
                  <tr className="border-b border-slate-100 text-[9px] font-extrabold uppercase text-slate-400 tracking-wider">
                    <th className="pb-3 pr-4">Khách hàng</th>
                    <th className="pb-3 px-4">Thời gian</th>
                    <th className="pb-3 px-4">Hình thức</th>
                    <th className="pb-3 pl-4 text-right">Trạng thái</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {bookings.slice(0, 5).map((bk) => (
                    <tr key={bk.id} className="hover:bg-slate-55/30 transition-colors">
                      <td className="py-4 pr-4">
                        <span className="text-slate-800 font-extrabold uppercase block">{bk.guestName}</span>
                        <span className="text-[9px] text-slate-400 block mt-1">{bk.bookingReference} • {bk.roomType}</span>
                      </td>
                      <td className="py-4 px-4 font-semibold text-slate-700">
                        {bk.checkInDate} <br />
                        <span className="text-[9px] text-slate-400 font-normal">{bk.nights} đêm</span>
                      </td>
                      <td className="py-4 px-4 text-slate-500 font-extrabold uppercase tracking-wider text-[9px]">
                        {bk.checkInMethod === 'Face Recognition' || bk.checkInMethod === 'FaceID' ? 'Xác minh khuôn mặt' : bk.checkInMethod}
                      </td>
                      <td className="py-4 pl-4 text-right">
                        <span className={`inline-block px-2.5 py-0.5 text-[8.5px] font-extrabold uppercase tracking-widest rounded-lg ${
                          bk.status === 'Checked In'
                            ? 'bg-blue-50 text-blue-600 border border-blue-100'
                            : bk.status === 'Confirmed'
                            ? 'bg-emerald-50 text-emerald-600 border border-emerald-100'
                            : 'bg-slate-50 text-slate-600 border border-slate-200/60'
                        }`}>
                          {bk.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Right Column (Identity & AI Assistant) */}
        <div className="space-y-6">
          {/* Quick Actions Panel */}
          <div className="bg-white border border-slate-100 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.015)] p-6">
            <h4 className="text-xs font-black uppercase tracking-widest text-slate-800 border-b border-slate-100 pb-3 mb-4">Lối tắt thao tác</h4>
            <div className="space-y-3">
              <button
                onClick={handleOpenWalkIn}
                className="w-full py-3.5 bg-slate-50 hover:bg-primary text-slate-700 hover:text-white text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer border border-slate-200/60 rounded-xl flex items-center justify-center gap-2"
              >
                Đặt phòng Walk-in
              </button>
              {isManager && (
                <button
                  onClick={handleOpenAddRoom}
                  className="w-full py-3.5 bg-slate-50 hover:bg-primary text-slate-700 hover:text-white text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer border border-slate-200/60 rounded-xl flex items-center justify-center gap-2"
                >
                  Thêm loại phòng mới
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {activeRoomTypes.length > 0 && (
        <div className="bg-white border border-slate-100 rounded-2xl p-6 shadow-[0_8px_30px_rgb(0,0,0,0.015)]">
          <div className="flex justify-between items-center border-b border-slate-100 pb-4 mb-6">
            <div>
              <h4 className="text-xs font-black text-slate-800 uppercase tracking-widest">Danh mục hạng phòng hiện có</h4>
              <p className="text-[9px] text-slate-400 uppercase tracking-widest mt-1">Cấu hình loại phòng lưu trú cao cấp tại hệ thống</p>
            </div>
            <div className="flex gap-2">
              <button
                onClick={prevCarousel}
                className="p-2 bg-slate-50 hover:bg-primary text-slate-700 hover:text-white transition-colors cursor-pointer border border-slate-200/60 rounded-xl flex items-center"
              >
                <ChevronLeft size={15} />
              </button>
              <button
                onClick={nextCarousel}
                className="p-2 bg-slate-50 hover:bg-primary text-slate-700 hover:text-white transition-colors cursor-pointer border border-slate-200/60 rounded-xl flex items-center"
              >
                <ChevronRight size={15} />
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {activeRoomTypes.slice(carouselIndex, carouselIndex + 3).concat(
              activeRoomTypes.slice(0, Math.max(0, 3 - (activeRoomTypes.length - carouselIndex)))
            ).slice(0, Math.min(3, activeRoomTypes.length)).map((room) => (
              <div key={room.id} className="bg-slate-50/60 border border-slate-150 rounded-2xl overflow-hidden flex flex-col justify-between shadow-sm hover:shadow-md transition-all">
                <img
                  src={room.primaryImageUrl || 'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=600&q=80'}
                  alt={room.name}
                  className="w-full h-40 object-cover border-b border-slate-200/60 filter brightness-95"
                />
                <div className="p-4 space-y-2 text-left">
                  <span className="text-[9px] text-primary font-extrabold uppercase tracking-widest">THE IRIS SUITE</span>
                  <h5 className="text-sm font-bold text-slate-800 uppercase tracking-wider">{room.name}</h5>
                  <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">
                    {room.bedType || 'King Bed'} • {room.roomSize || room.roomsize || 35} m²
                  </p>
                  <div className="flex justify-between items-end pt-3 border-t border-slate-200/60">
                    <div>
                      <span className="text-[8px] text-slate-400 font-bold uppercase block">Đơn giá cơ bản</span>
                      <span className="text-xs font-black text-primary">
                        {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(room.basePrice || room.baseprice || 0)}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default StaffOverview;
