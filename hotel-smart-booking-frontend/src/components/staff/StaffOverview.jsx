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
  handleOpenWalkIn
}) => {
  const [carouselIndex, setCarouselIndex] = useState(0);

  // Calcs
  const confirmedCount = bookings.filter(b => b.status === 'Confirmed').length;
  const checkedInCount = bookings.filter(b => b.status === 'Checked In').length;

  const nextCarousel = () => {
    if (roomTypes.length > 0) {
      setCarouselIndex((prev) => (prev + 1) % roomTypes.length);
    }
  };

  const prevCarousel = () => {
    if (roomTypes.length > 0) {
      setCarouselIndex((prev) => (prev - 1 + roomTypes.length) % roomTypes.length);
    }
  };

  return (
    <div className="space-y-8 animate-scale-in text-left">
      {/* Welcome Banner */}
      <div className="h-64 rounded-none overflow-hidden relative border border-neutral-900 shadow-xl">
        <img
          src="https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1000&h=400&fit=crop"
          alt="Luxury hotel lobby"
          className="absolute inset-0 w-full h-full object-cover filter brightness-75"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-[#070708] via-[#0d0d0f]/50 to-transparent" />
        <div className="relative h-full p-8 flex flex-col justify-between items-start text-left">
          <div>
            <span className="inline-flex items-center gap-1 px-3 py-1 bg-primary text-white text-[9px] font-black uppercase tracking-widest">
              <Shield size={10} /> {isManager ? 'ADMIN ACCESS' : 'OPERATOR ACCESS'}
            </span>
            <h1 className="mt-4 text-white text-3xl font-black uppercase tracking-wider italic leading-none">
              Chào mừng trở lại, {currentUser.fullName.split(' ').pop()}
            </h1>
            <p className="mt-2 text-slate-300 text-xs font-bold uppercase tracking-wider">
              Hệ thống Elysian Hub của bạn đã sẵn sàng hoạt động ngày hôm nay.
            </p>
          </div>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-[#0f0f12] border border-neutral-900 p-6 shadow-md relative overflow-hidden flex flex-col justify-between h-28">
          <div className="flex justify-between items-start">
            <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">YÊU CẦU CHECK-IN</span>
            <span className="text-[9px] font-black tracking-widest text-primary uppercase bg-primary/10 px-2 py-0.5">ACTIVE</span>
          </div>
          <div className="flex items-baseline gap-2 mt-2">
            <span className="text-3xl font-black text-white">{confirmedCount}</span>
            <span className="text-[10px] text-slate-500 font-bold uppercase">đơn chờ</span>
          </div>
        </div>

        <div className="bg-[#0f0f12] border border-neutral-900 p-6 shadow-md relative overflow-hidden flex flex-col justify-between h-28">
          <div className="flex justify-between items-start">
            <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">ĐANG LƯU TRÚ</span>
          </div>
          <div className="flex items-baseline gap-2 mt-2">
            <span className="text-3xl font-black text-white">{checkedInCount}</span>
            <span className="text-[10px] text-slate-500 font-bold uppercase">phòng hoạt động</span>
          </div>
        </div>

        <div className="bg-[#0f0f12] border border-neutral-900 p-6 shadow-md relative overflow-hidden flex flex-col justify-between h-28">
          <div className="flex justify-between items-start">
            <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">TỶ LỆ LẤP ĐẦY</span>
          </div>
          <div className="flex items-baseline gap-2 mt-2">
            <span className="text-3xl font-black text-white">78.5%</span>
          </div>
        </div>

        <div className="bg-[#0f0f12] border border-neutral-900 p-6 shadow-md relative overflow-hidden flex flex-col justify-between h-28">
          <div className="flex justify-between items-start">
            <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">DOANH THU DỰ TÍNH</span>
          </div>
          <div className="flex items-baseline gap-2 mt-2">
            <span className="text-2xl font-black text-primary">147.0M đ</span>
          </div>
        </div>
      </div>

      {/* Main Two-Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column (Recent Bookings / Guest List) */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-[#0f0f12] border border-neutral-900 shadow-md p-6">
            <div className="flex justify-between items-center border-b border-neutral-900 pb-4 mb-4">
              <div>
                <h4 className="text-xs font-black text-white uppercase tracking-widest">Danh sách check-in gần đây</h4>
                <p className="text-[9px] text-slate-500 uppercase tracking-widest mt-1">Quản lý trực tiếp các đặt phòng đang diễn ra</p>
              </div>
              <button
                onClick={() => setActiveTab('operations')}
                className="text-primary hover:underline font-black text-[10px] uppercase tracking-wider border-none bg-transparent cursor-pointer"
              >
                Xử lý ngay
              </button>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full border-collapse text-left text-xs text-slate-400">
                <thead>
                  <tr className="border-b border-neutral-900 text-[9px] font-black uppercase text-slate-500 tracking-wider">
                    <th className="pb-3 pr-4">Khách hàng</th>
                    <th className="pb-3 px-4">Thời gian</th>
                    <th className="pb-3 px-4">Hình thức</th>
                    <th className="pb-3 pl-4 text-right">Trạng thái</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-900/60">
                  {bookings.slice(0, 5).map((bk) => (
                    <tr key={bk.id} className="hover:bg-white/5 transition-colors">
                      <td className="py-4 pr-4">
                        <span className="text-white font-black uppercase block">{bk.guestName}</span>
                        <span className="text-[9px] text-slate-500 block mt-0.5">{bk.bookingReference} • {bk.roomType}</span>
                      </td>
                      <td className="py-4 px-4 font-medium">
                        {bk.checkInDate} <br />
                        <span className="text-[9px] text-slate-500">{bk.nights} đêm</span>
                      </td>
                      <td className="py-4 px-4 text-primary font-black uppercase tracking-wider text-[9px]">
                        {bk.checkInMethod === 'Face Recognition' || bk.checkInMethod === 'FaceID' ? 'FaceID eKYC' : bk.checkInMethod}
                      </td>
                      <td className="py-4 pl-4 text-right">
                        <span className={`inline-block px-2 py-0.5 text-[8px] font-black uppercase tracking-widest ${
                          bk.status === 'Checked In'
                            ? 'bg-blue-900/30 text-blue-400 border border-blue-900/50'
                            : bk.status === 'Confirmed'
                            ? 'bg-green-900/30 text-green-400 border border-green-900/50'
                            : 'bg-neutral-800 text-neutral-400'
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

        {/* Right Column (Identity, Quick Action, Financial Overview) */}
        <div className="space-y-6">
          {/* Staff Identity Verified Card */}
          <div className="bg-primary p-6 shadow-md text-white flex flex-col justify-between h-56 relative overflow-hidden">
            <div className="absolute right-4 top-4 opacity-15">
              <UserCheck size={80} />
            </div>
            <div>
              <span className="text-[9px] font-black uppercase tracking-widest bg-white/20 px-2 py-0.5 inline-block">STAFF IDENTIFIED</span>
              <h4 className="text-sm font-black uppercase tracking-wider mt-3">Quyền truy cập hợp lệ</h4>
              <p className="text-[10px] text-white/80 font-bold uppercase tracking-wider mt-1.5 leading-relaxed">
                Tài khoản của bạn đã được đối sánh sinh trắc học và cấp quyền thực hiện các nhiệm vụ lễ tân hoặc quản trị trên Elysian Hub.
              </p>
            </div>
            <div className="flex gap-3 mt-4 border-t border-white/20 pt-4">
              <div className="text-[8px] font-black uppercase tracking-wider">
                ID Nhân viên: EL-2026
              </div>
            </div>
          </div>

          {/* Quick Actions Panel */}
          <div className="bg-[#0f0f12] border border-neutral-900 shadow-md p-6">
            <h4 className="text-xs font-black uppercase tracking-widest text-white border-b border-neutral-900 pb-3 mb-4">Lối tắt thao tác</h4>
            <div className="space-y-3">
              <button
                onClick={() => setActiveTab('operations')}
                className="w-full py-3.5 bg-neutral-900 hover:bg-primary text-white text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer border border-neutral-850 flex items-center justify-center gap-2"
              >
                Vận hành sảnh (Check-in)
              </button>
              <button
                onClick={handleOpenWalkIn}
                className="w-full py-3.5 bg-neutral-900 hover:bg-primary text-white text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer border border-neutral-850 flex items-center justify-center gap-2"
              >
                Đặt phòng Walk-in
              </button>
              {isManager && (
                <button
                  onClick={handleOpenAddRoom}
                  className="w-full py-3.5 bg-neutral-900 hover:bg-primary text-white text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer border border-neutral-850 flex items-center justify-center gap-2"
                >
                  Thêm loại phòng mới
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Curated Room Types carousel */}
      {roomTypes.length > 0 && (
        <div className="bg-[#0f0f12] border border-neutral-900 p-6 shadow-md">
          <div className="flex justify-between items-center border-b border-neutral-900 pb-4 mb-6">
            <div>
              <h4 className="text-xs font-black text-white uppercase tracking-widest">Danh mục hạng phòng hiện có</h4>
              <p className="text-[9px] text-slate-500 uppercase tracking-widest mt-1">Cấu hình loại phòng lưu trú cao cấp tại hệ thống</p>
            </div>
            <div className="flex gap-2">
              <button
                onClick={prevCarousel}
                className="p-1.5 bg-neutral-900 hover:bg-primary text-white transition-colors cursor-pointer border border-neutral-800"
              >
                <ChevronLeft size={16} />
              </button>
              <button
                onClick={nextCarousel}
                className="p-1.5 bg-neutral-900 hover:bg-primary text-white transition-colors cursor-pointer border border-neutral-800"
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {roomTypes.slice(carouselIndex, carouselIndex + 3).concat(
              roomTypes.slice(0, Math.max(0, 3 - (roomTypes.length - carouselIndex)))
            ).slice(0, Math.min(3, roomTypes.length)).map((room) => (
              <div key={room.id} className="bg-neutral-950 border border-neutral-900 overflow-hidden flex flex-col justify-between">
                <img
                  src={room.primaryImageUrl || 'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=600&q=80'}
                  alt={room.name}
                  className="w-full h-40 object-cover border-b border-neutral-900 filter brightness-90"
                />
                <div className="p-4 space-y-2">
                  <span className="text-[9px] text-primary font-black uppercase tracking-widest">ELYSIAN SUITE</span>
                  <h5 className="text-sm font-black text-white uppercase tracking-wider">{room.name}</h5>
                  <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">
                    {room.bedType || 'King Bed'} • {room.roomSize || room.roomsize || 35} m²
                  </p>
                  <div className="flex justify-between items-end pt-3 border-t border-neutral-900">
                    <div>
                      <span className="text-[8px] text-slate-500 font-bold uppercase block">Đơn giá cơ bản</span>
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
