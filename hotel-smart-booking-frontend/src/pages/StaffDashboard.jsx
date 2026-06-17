import { useState, useEffect } from 'react';
import { getRoomTypes } from '../services/roomService';
import { createRoomType, updateRoomType, deleteRoomType } from '../services/roomManagementService';
import { getBookingHistory, getAllBookings, checkInBooking, checkOutBooking } from '../services/bookingService';
import Profile from './Profile';
import { useToast, ToastContainer } from '../components/Toast';

// Mock Bookings Data for Receptionist/Manager Operation simulation
const INITIAL_MOCK_BOOKINGS = [
  { id: 101, bookingReference: 'BK20260616173108820', guestName: 'Nguyễn Văn Minh', email: 'minh.nguyen@example.com', roomType: 'Suite River View', quantity: 1, checkInDate: '2026-06-16', checkOutDate: '2026-07-01', nights: 15, totalAmount: 75000000, status: 'Confirmed', checkInMethod: 'Face Recognition' },
  { id: 102, bookingReference: 'BK20260611183213122', guestName: 'Trần Thị Thảo', email: 'thao.tran@example.com', roomType: 'Premium Triple Room City View', quantity: 1, checkInDate: '2026-06-11', checkOutDate: '2026-06-21', nights: 10, totalAmount: 16000000, status: 'Confirmed', checkInMethod: 'QR Code' },
  { id: 103, bookingReference: 'BK20260611173944273', guestName: 'Lê Hoàng Nam', email: 'nam.le@example.com', roomType: 'Standard No Window', quantity: 1, checkInDate: '2026-06-11', checkOutDate: '2026-06-21', nights: 10, totalAmount: 16000000, status: 'Checked In', checkInMethod: 'Manual' },
  { id: 104, bookingReference: 'BK20260615104499120', guestName: 'Phạm Minh Đức', email: 'duc.pham@example.com', roomType: 'Suite River View', quantity: 2, checkInDate: '2026-06-16', checkOutDate: '2026-06-20', nights: 4, totalAmount: 40000000, status: 'Confirmed', checkInMethod: 'Face Recognition', bookingType: 'Group' },
];

export default function StaffDashboard({ setActivePage }) {
  const { toasts, showToast, dismissToast } = useToast();
  
  // Auth state
  const [currentUser, setCurrentUser] = useState(() => {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : { fullName: 'Nhân viên Elysian', role: 'receptionist', email: 'staff@elysian.com' };
  });

  const isManager = currentUser.role === 'manager';
  
  // Dashboard Tabs Navigation
  const [activeTab, setActiveTab] = useState('overview');

  // Simulated Booking states
  const [bookings, setBookings] = useState(INITIAL_MOCK_BOOKINGS);
  const [searchQuery, setSearchQuery] = useState('');
  
  // Live Room Types states for Manager CRUD
  const [roomTypes, setRoomTypes] = useState([]);
  const [isRoomFormOpen, setIsRoomFormOpen] = useState(false);
  const [editingRoom, setEditingRoom] = useState(null);
  const [roomFormData, setRoomFormData] = useState({
    name: '',
    basePrice: '',
    adultCapacity: '2',
    childCapacity: '1',
    bedType: 'Giường Đôi King Size',
    roomSize: '35',
    status: 'Active',
    description: ''
  });
  const [roomImage, setRoomImage] = useState(null);
  const [isSubmittingRoom, setIsSubmittingRoom] = useState(false);

  // Simulated Camera / FaceID Scanning Modal
  const [scanningBooking, setScanningBooking] = useState(null);
  const [scanType, setScanType] = useState(null); // 'face' | 'qr'
  const [isScanning, setIsScanning] = useState(false);
  const [selectedBooking, setSelectedBooking] = useState(null);

  useEffect(() => {
    fetchRoomTypes();
    fetchRealBookings();
  }, []);

  const fetchRealBookings = async () => {
    try {
      let allBookings = [];
      try {
        const response = await getAllBookings();
        if (response && response.data) {
          allBookings = response.data;
        }
      } catch (err) {
        console.error('Lỗi khi tải toàn bộ đặt phòng từ backend:', err);
        // Fallback: try getBookingHistory or localStorage just in case
        try {
          const response = await getBookingHistory();
          if (response && response.data) {
            allBookings = [...response.data];
          }
        } catch (e2) {}
        try {
          const localCreated = JSON.parse(localStorage.getItem('hotel_all_bookings') || '[]');
          localCreated.forEach(localBk => {
            if (!allBookings.some(b => b.bookingId === localBk.bookingId)) {
              allBookings.push(localBk);
            }
          });
        } catch (e3) {}
      }

      if (allBookings.length > 0) {
        const realMapped = allBookings.map(bk => {
          const localStatus = localStorage.getItem(`booking_status_${bk.bookingId}`) || bk.status;
          return {
            id: bk.bookingId,
            bookingReference: bk.bookingNumber || bk.bookingReference || `BK-${bk.bookingId}`,
            guestName: bk.guestName || 'Khách hàng Elysian',
            email: bk.guestEmail || bk.email || '',
            roomType: bk.roomType || bk.roomTypeName,
            quantity: bk.quantity || 1,
            checkInDate: bk.checkInDate,
            checkOutDate: bk.checkOutDate,
            nights: bk.nights,
            totalAmount: bk.totalAmount,
            status: localStatus === 'Cancelled' ? 'Cancelled' : localStatus === 'Checked-in' || localStatus === 'Checked In' ? 'Checked In' : localStatus === 'Checked-out' || localStatus === 'Checked Out' ? 'Checked Out' : 'Confirmed',
            checkInMethod: bk.checkInMethod || 'Manual',
            bookingType: bk.bookingType || 'Online',
            guestPhone: bk.guestPhone || '',
            specialRequests: bk.specialRequests || '',
            numberOfAdults: bk.numberOfAdults || 1,
            numberOfChildren: bk.numberOfChildren || 0
          };
        });

        setBookings(prev => {
          const filteredMocks = prev.filter(mock => 
            !realMapped.some(real => real.bookingReference === mock.bookingReference)
          );
          return [...realMapped, ...filteredMocks];
        });
      }
    } catch (err) {
      console.error('Lỗi trong fetchRealBookings:', err);
    }
  };

  const fetchRoomTypes = async () => {
    try {
      const response = await getRoomTypes();
      if (response && response.data && response.data.content) {
        setRoomTypes(response.data.content);
      }
    } catch (err) {
      console.error('Lỗi khi tải loại phòng:', err);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    showToast('Đăng xuất thành công!', 'success');
    setTimeout(() => {
      setActivePage('home');
    }, 1000);
  };

  // Check-in & Check-out simulations
  const startScanner = (booking, type) => {
    setScanningBooking(booking);
    setScanType(type);
    setIsScanning(true);
    // Auto complete scan after 2.5s simulation
    setTimeout(() => {
      setIsScanning(false);
    }, 2500);
  };

  const completeScannerAction = async () => {
    const isCheckIn = scanningBooking.status === 'Confirmed';
    const nextStatus = isCheckIn ? 'Checked In' : 'Checked Out';
    
    try {
      if (isCheckIn) {
        await checkInBooking(scanningBooking.id);
      } else {
        await checkOutBooking(scanningBooking.id);
      }
      
      // Save to localStorage so it persists across logins
      localStorage.setItem(`booking_status_${scanningBooking.id}`, nextStatus);
      if (nextStatus === 'Checked In') {
        localStorage.setItem(`booking_actualcheckin_${scanningBooking.id}`, new Date().toISOString());
      } else if (nextStatus === 'Checked Out') {
        localStorage.setItem(`booking_actualcheckout_${scanningBooking.id}`, new Date().toISOString());
      }

      setBookings(prev => prev.map(bk => 
        bk.id === scanningBooking.id ? { ...bk, status: nextStatus } : bk
      ));
      
      showToast(`Đã cập nhật trạng thái đơn ${scanningBooking.bookingReference} sang ${nextStatus === 'Checked In' ? 'ĐÃ NHẬN PHÒNG' : 'ĐÃ TRẢ PHÒNG'} thành công!`, 'success');
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Có lỗi xảy ra khi cập nhật trạng thái.', 'error');
    } finally {
      setIsScanning(false);
      setScanningBooking(null);
      setScanType(null);
    }
  };

  const handleDirectCheckInOut = async (booking, targetStatus) => {
    const isCheckIn = targetStatus === 'Checked In';
    try {
      if (isCheckIn) {
        await checkInBooking(booking.id);
      } else {
        await checkOutBooking(booking.id);
      }

      // Save to localStorage so it persists across logins
      localStorage.setItem(`booking_status_${booking.id}`, targetStatus);
      if (targetStatus === 'Checked In') {
        localStorage.setItem(`booking_actualcheckin_${booking.id}`, new Date().toISOString());
      } else if (targetStatus === 'Checked Out') {
        localStorage.setItem(`booking_actualcheckout_${booking.id}`, new Date().toISOString());
      }

      setBookings(prev => prev.map(bk => 
        bk.id === booking.id ? { ...bk, status: targetStatus } : bk
      ));
      showToast(`Đã chuyển trạng thái sang ${targetStatus === 'Checked In' ? 'ĐÃ NHẬN PHÒNG' : 'ĐÃ TRẢ PHÒNG'}!`, 'success');
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Có lỗi xảy ra khi cập nhật trạng thái.', 'error');
    }
  };

  // Filter bookings for receptionist
  const filteredBookings = bookings.filter(bk => 
    bk.guestName.toLowerCase().includes(searchQuery.toLowerCase()) || 
    bk.bookingReference.toLowerCase().includes(searchQuery.toLowerCase()) ||
    bk.email.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Room Type CRUD Handlers
  const handleOpenAddRoom = () => {
    setEditingRoom(null);
    setRoomFormData({
      name: '',
      basePrice: '',
      adultCapacity: '2',
      childCapacity: '1',
      bedType: 'Giường Đôi King Size',
      roomSize: '35',
      status: 'Active',
      description: ''
    });
    setRoomImage(null);
    setIsRoomFormOpen(true);
  };

  const handleOpenEditRoom = (room) => {
    setEditingRoom(room);
    setRoomFormData({
      name: room.name || '',
      basePrice: room.basePrice || room.baseprice || '',
      adultCapacity: String(room.adultCapacity || room.adultcapacity || '2'),
      childCapacity: String(room.childCapacity || room.childcapacity || '1'),
      bedType: room.bedType || 'Giường Đôi King Size',
      roomSize: String(room.roomSize || room.roomsize || '35'),
      status: room.status || 'Active',
      description: room.description || ''
    });
    setRoomImage(null);
    setIsRoomFormOpen(true);
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setRoomImage(e.target.files[0]);
    }
  };

  const handleRoomSubmit = async (e) => {
    e.preventDefault();
    if (!roomFormData.name || !roomFormData.basePrice) {
      showToast('Vui lòng nhập tên và giá loại phòng', 'warning');
      return;
    }

    setIsSubmittingRoom(true);
    try {
      const formData = new FormData();
      formData.append('name', roomFormData.name);
      formData.append('basePrice', roomFormData.basePrice);
      formData.append('adultCapacity', roomFormData.adultCapacity);
      formData.append('childCapacity', roomFormData.childCapacity);
      formData.append('bedType', roomFormData.bedType);
      formData.append('roomSize', roomFormData.roomSize);
      formData.append('status', roomFormData.status);
      formData.append('description', roomFormData.description);
      if (roomImage) {
        formData.append('image', roomImage);
      }

      if (editingRoom) {
        await updateRoomType(editingRoom.id, formData);
        showToast(`Đã cập nhật loại phòng ${roomFormData.name} thành công!`, 'success');
      } else {
        await createRoomType(formData);
        showToast(`Đã thêm loại phòng ${roomFormData.name} thành công!`, 'success');
      }
      setIsRoomFormOpen(false);
      fetchRoomTypes();
    } catch (err) {
      console.error('Lỗi khi lưu loại phòng:', err);
      // Fallback update local list if backend mock mode
      const updatedMockList = [...roomTypes];
      if (editingRoom) {
        const idx = updatedMockList.findIndex(r => r.id === editingRoom.id);
        if (idx !== -1) {
          updatedMockList[idx] = { ...editingRoom, ...roomFormData, basePrice: parseFloat(roomFormData.basePrice) };
        }
      } else {
        updatedMockList.push({ id: Date.now(), ...roomFormData, basePrice: parseFloat(roomFormData.basePrice), primaryImageUrl: 'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=600&q=80' });
      }
      setRoomTypes(updatedMockList);
      showToast('Đã cập nhật danh sách phòng cục bộ.', 'success');
      setIsRoomFormOpen(false);
    } finally {
      setIsSubmittingRoom(false);
    }
  };

  const handleDeleteRoom = async (id, name) => {
    if (window.confirm(`Bạn có chắc chắn muốn xóa loại phòng "${name}"?`)) {
      try {
        await deleteRoomType(id);
        showToast(`Đã xóa loại phòng "${name}" thành công!`, 'success');
        fetchRoomTypes();
      } catch (err) {
        console.error('Lỗi khi xóa loại phòng:', err);
        setRoomTypes(prev => prev.filter(r => r.id !== id));
        showToast('Đã cập nhật danh sách phòng cục bộ.', 'success');
      }
    }
  };

  return (
    <>
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />
      
      <div className="min-h-screen bg-gray-50 flex font-['Montserrat'] text-left pt-0">
        
        {/* SIDEBAR */}
        <aside className="w-72 bg-slate-950 text-white flex flex-col justify-between fixed top-0 bottom-0 left-0 border-r border-slate-900 z-30">
          <div>
            {/* Header info */}
            <div className="p-6 border-b border-slate-900 bg-slate-900/40">
              <span className="text-[9px] font-black tracking-[0.25em] text-primary uppercase block mb-1">TRANG QUẢN TRỊ</span>
              <h2 className="text-lg font-black uppercase tracking-wider text-white m-0">ELYSIAN HUB</h2>
              
              <div className="flex items-center gap-3 mt-4">
                <div className="w-10 h-10 rounded-none border border-primary bg-primary/10 flex items-center justify-center font-black text-sm text-primary">
                  {currentUser.fullName.slice(0, 2).toUpperCase()}
                </div>
                <div className="min-w-0">
                  <p className="text-xs font-black text-white truncate m-0 uppercase tracking-wide">{currentUser.fullName}</p>
                  <span className="inline-block mt-1 px-2 py-0.5 text-[8px] font-black tracking-widest text-primary bg-primary/10 uppercase">
                    {isManager ? 'QUẢN LÝ' : 'LỄ TÂN'}
                  </span>
                </div>
              </div>
            </div>

            {/* Nav Menu */}
            <nav className="p-4 space-y-1">
              <button 
                onClick={() => setActiveTab('overview')}
                className={`w-full py-3.5 px-4 text-xs font-bold uppercase tracking-wider border-none flex items-center gap-3 cursor-pointer transition-all duration-150 ${
                  activeTab === 'overview' ? 'bg-primary text-white font-extrabold' : 'bg-transparent text-slate-400 hover:text-white hover:bg-slate-900/40'
                }`}
              >
                <span className="material-symbols-outlined text-base">dashboard</span>
                Tổng quan
              </button>

              <button 
                onClick={() => setActiveTab('operations')}
                className={`w-full py-3.5 px-4 text-xs font-bold uppercase tracking-wider border-none flex items-center gap-3 cursor-pointer transition-all duration-150 ${
                  activeTab === 'operations' ? 'bg-primary text-white font-extrabold' : 'bg-transparent text-slate-400 hover:text-white hover:bg-slate-900/40'
                }`}
              >
                <span className="material-symbols-outlined text-base">how_to_reg</span>
                Vận hành sảnh (Check-in)
              </button>

              {isManager && (
                <>
                  <button 
                    onClick={() => setActiveTab('rooms')}
                    className={`w-full py-3.5 px-4 text-xs font-bold uppercase tracking-wider border-none flex items-center gap-3 cursor-pointer transition-all duration-150 ${
                      activeTab === 'rooms' ? 'bg-primary text-white font-extrabold' : 'bg-transparent text-slate-400 hover:text-white hover:bg-slate-900/40'
                    }`}
                  >
                    <span className="material-symbols-outlined text-base">meeting_room</span>
                    Quản lý loại phòng
                  </button>

                  <button 
                    onClick={() => setActiveTab('reports')}
                    className={`w-full py-3.5 px-4 text-xs font-bold uppercase tracking-wider border-none flex items-center gap-3 cursor-pointer transition-all duration-150 ${
                      activeTab === 'reports' ? 'bg-primary text-white font-extrabold' : 'bg-transparent text-slate-400 hover:text-white hover:bg-slate-900/40'
                    }`}
                  >
                    <span className="material-symbols-outlined text-base">query_stats</span>
                    Báo cáo doanh thu
                  </button>
                </>
              )}

              <button 
                onClick={() => setActiveTab('settings')}
                className={`w-full py-3.5 px-4 text-xs font-bold uppercase tracking-wider border-none flex items-center gap-3 cursor-pointer transition-all duration-150 ${
                  activeTab === 'settings' ? 'bg-primary text-white font-extrabold' : 'bg-transparent text-slate-400 hover:text-white hover:bg-slate-900/40'
                }`}
              >
                <span className="material-symbols-outlined text-base">manage_accounts</span>
                Hồ sơ cá nhân
              </button>
            </nav>
          </div>

          {/* Bottom logout */}
          <div className="p-4 border-t border-slate-900">
            <button 
              onClick={handleLogout}
              className="w-full py-3 px-4 text-xs font-bold uppercase tracking-wider text-slate-400 hover:text-primary hover:bg-[#ffe0dd]/5 transition-colors border border-dashed border-slate-800 flex items-center justify-center gap-2 cursor-pointer bg-transparent"
            >
              <span className="material-symbols-outlined text-base">logout</span>
              Đăng xuất
            </button>
          </div>
        </aside>

        {/* MAIN PANEL CONTENT */}
        <main className="ml-72 flex-1 min-h-screen p-6 md:p-10 flex flex-col gap-6">
          
          {/* TỔNG QUAN (OVERVIEW) */}
          {activeTab === 'overview' && (
            <div className="space-y-6 animate-scale-in">
              <div>
                <h3 className="font-headline-lg text-lg text-primary uppercase italic tracking-wider m-0">TỔNG QUAN HỆ THỐNG</h3>
                <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest mt-1">Báo cáo tóm tắt trạng thái vận hành hôm nay</p>
              </div>

              {/* Stats Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                <div className="bg-white border border-outline-variant p-6 shadow-md flex justify-between items-center">
                  <div>
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Tỷ lệ lấp đầy</span>
                    <span className="text-2xl font-black text-slate-950 mt-1 block">78.5%</span>
                  </div>
                  <span className="material-symbols-outlined text-3xl text-primary bg-primary/5 p-3">percent</span>
                </div>

                <div className="bg-white border border-outline-variant p-6 shadow-md flex justify-between items-center">
                  <div>
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Yêu cầu check-in</span>
                    <span className="text-2xl font-black text-slate-950 mt-1 block">{bookings.filter(b => b.status === 'Confirmed').length} đơn</span>
                  </div>
                  <span className="material-symbols-outlined text-3xl text-blue-600 bg-blue-50 p-3">how_to_reg</span>
                </div>

                <div className="bg-white border border-outline-variant p-6 shadow-md flex justify-between items-center">
                  <div>
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Đang lưu trú</span>
                    <span className="text-2xl font-black text-slate-950 mt-1 block">{bookings.filter(b => b.status === 'Checked In').length} phòng</span>
                  </div>
                  <span className="material-symbols-outlined text-3xl text-green-600 bg-green-50 p-3">vpn_key</span>
                </div>

                <div className="bg-white border border-outline-variant p-6 shadow-md flex justify-between items-center">
                  <div>
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Doanh thu dự tính</span>
                    <span className="text-lg font-black text-primary mt-1 block">147.0M đ</span>
                  </div>
                  <span className="material-symbols-outlined text-3xl text-primary bg-primary/5 p-3">payments</span>
                </div>
              </div>

              {/* Main row */}
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Active Check-in queue */}
                <div className="lg:col-span-2 bg-white border border-outline-variant shadow-md p-6">
                  <h4 className="text-xs font-black text-slate-900 uppercase tracking-widest border-b border-slate-100 pb-3 mb-4 flex justify-between items-center">
                    <span>DANH SÁCH CHECK-IN GẦN ĐÂY</span>
                    <button onClick={() => setActiveTab('operations')} className="text-primary hover:underline font-bold text-[10px] uppercase tracking-wider border-none bg-transparent cursor-pointer">Xử lý ngay</button>
                  </h4>

                  <div className="divide-y divide-slate-100">
                    {bookings.slice(0, 3).map((bk) => (
                      <div key={bk.id} className="py-4 flex justify-between items-center gap-4 text-xs font-bold text-slate-700">
                        <div>
                          <p className="text-slate-900 font-black uppercase truncate m-0">{bk.guestName}</p>
                          <span className="text-[9px] text-slate-400 mt-0.5 block">{bk.bookingReference} • {bk.roomType}</span>
                        </div>
                        <div className="text-right shrink-0">
                          <span className={`px-2 py-0.5 text-[8px] font-black uppercase tracking-widest ${bk.status === 'Checked In' ? 'bg-blue-100 text-blue-700' : 'bg-green-100 text-green-700'}`}>
                            {bk.status}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Quick actions Panel */}
                <div className="bg-slate-900 text-white border border-slate-950 shadow-md p-6 flex flex-col justify-between">
                  <div>
                    <h4 className="text-xs font-black uppercase tracking-widest text-primary border-b border-slate-800 pb-3 mb-4">Lối tắt nhanh</h4>
                    <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider leading-relaxed mb-6">Truy cập nhanh các chức năng vận hành khách sạn thông minh.</p>
                  </div>
                  
                  <div className="space-y-3">
                    <button onClick={() => setActiveTab('operations')} className="w-full py-3 bg-slate-800 text-white hover:bg-primary text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer border-none flex items-center justify-center gap-2">
                      <span className="material-symbols-outlined text-sm">qr_code_scanner</span> Quét QR / FaceID
                    </button>
                    {isManager && (
                      <button onClick={handleOpenAddRoom} className="w-full py-3 bg-slate-800 text-white hover:bg-primary text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer border-none flex items-center justify-center gap-2">
                        <span className="material-symbols-outlined text-sm">add_box</span> Thêm loại phòng mới
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* VẬN HÀNH SẢNH / CHECK-IN / CHECK-OUT */}
          {activeTab === 'operations' && (
            <div className="space-y-6 animate-scale-in">
              <div className="flex flex-col sm:flex-row sm:justify-between sm:items-end gap-4">
                <div>
                  <h3 className="font-headline-lg text-lg text-primary uppercase italic tracking-wider m-0">VẬN HÀNH SẢNH & CHECK-IN</h3>
                  <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest mt-1">Tìm kiếm khách hàng làm thủ tục nhận phòng hoặc trả phòng</p>
                </div>

                {/* Search Bar */}
                <div className="relative w-full sm:w-80">
                  <input 
                    type="text" 
                    placeholder="Mã đặt phòng, Tên khách, Email..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full bg-white border border-outline-variant p-3 pl-10 font-bold text-xs outline-none focus:border-primary"
                  />
                  <span className="material-symbols-outlined absolute left-3 top-3 text-lg text-slate-400">search</span>
                </div>
              </div>

              {/* Simulated Scanning Modal popup */}
              {isScanning && scanningBooking && (
                <div className="bg-slate-900 border-2 border-primary p-6 text-white text-center shadow-2xl relative animate-scale-in">
                  <div className="flex flex-col items-center justify-center gap-4 py-6">
                    <span className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin"></span>
                    <h4 className="text-sm font-black uppercase tracking-widest text-primary m-0">
                      {scanType === 'face' ? 'ĐANG QUÉT KHUÔN MẶT eKYC...' : 'ĐANG ĐỌC MÃ QR ĐOÀN...'}
                    </h4>
                    <p className="text-xs text-slate-400 font-bold uppercase tracking-wider max-w-sm">
                      Đang xác thực thông tin đối sánh của khách hàng: {scanningBooking.guestName}
                    </p>
                  </div>
                  <button onClick={completeScannerAction} className="bg-primary text-white font-black text-[10px] uppercase tracking-widest py-3.5 px-8 border-none cursor-pointer mt-4">
                    Hoàn tất đối sánh và xác thực
                  </button>
                </div>
              )}

              {/* Bookings Queue */}
              <div className="bg-white border border-outline-variant shadow-md overflow-hidden">
                <div className="p-4 border-b border-slate-100 bg-slate-50 text-[10px] font-black uppercase text-slate-500 tracking-wider">
                  Danh sách đặt phòng cần xử lý trong ngày
                </div>

                <div className="divide-y divide-slate-150">
                  {filteredBookings.length === 0 ? (
                    <div className="py-12 text-center text-slate-400 font-bold text-xs uppercase tracking-widest">
                      Không tìm thấy lịch trình đặt phòng nào phù hợp
                    </div>
                  ) : (
                    filteredBookings.map((bk) => {
                      const isConfirmed = bk.status === 'Confirmed';
                      const isCheckedIn = bk.status === 'Checked In';
                      const isCheckedOut = bk.status === 'Checked Out';
                      
                      return (
                        <div key={bk.id} className="p-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-6 hover:bg-slate-50 transition-colors">
                          <div className="space-y-1">
                            <span className="inline-block px-2 py-0.5 text-[8px] font-black tracking-widest text-slate-500 bg-slate-100 uppercase mb-1">
                              {bk.bookingType && bk.bookingType.toLowerCase() === 'group' ? 'ĐOÀN (GROUP)' : 'ĐƠN LẺ'}
                            </span>
                            <h5 className="text-sm font-black text-slate-900 uppercase tracking-wider m-0">
                              {bk.guestName}
                            </h5>
                            <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">
                              Mã: {bk.bookingReference} • {bk.roomType}
                            </p>
                            <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">
                              Ngày lưu trú: {bk.checkInDate} đến {bk.checkOutDate} ({bk.nights} đêm)
                            </p>
                          </div>

                          <div className="flex flex-wrap items-center gap-4 self-stretch md:self-auto justify-between md:justify-end">
                            {/* Details price & check-in method */}
                            <div className="text-left md:text-right shrink-0">
                              <span className="text-[8px] text-slate-400 font-bold uppercase tracking-wider block">Check-in bằng:</span>
                              <span className="text-[10px] text-primary font-black uppercase tracking-wider">{bk.checkInMethod === 'Face Recognition' ? 'FaceID eKYC' : bk.checkInMethod}</span>
                              <span className="text-xs font-black text-slate-900 block mt-1">
                                {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(bk.totalAmount)}
                              </span>
                            </div>

                            {/* Status badge */}
                            <span className={`px-2.5 py-1 text-[8px] font-black uppercase tracking-widest ${
                              isCheckedIn ? 'bg-blue-100 text-blue-700' : isConfirmed ? 'bg-green-100 text-green-700' : 'bg-slate-150 text-slate-500'
                            }`}>
                              {bk.status}
                            </span>

                            {/* Actions */}
                            <div className="flex gap-2">
                              <button 
                                onClick={() => setSelectedBooking(bk)}
                                className="bg-slate-100 hover:bg-slate-200 text-slate-800 text-[9px] font-black uppercase tracking-widest px-3 py-2 border-none cursor-pointer flex items-center gap-1"
                              >
                                <span className="material-symbols-outlined text-xs">info</span> Chi tiết
                              </button>
                              {isConfirmed && (
                                <>
                                  <button 
                                    onClick={() => startScanner(bk, bk.checkInMethod === 'Face Recognition' ? 'face' : 'qr')}
                                    className="bg-slate-900 hover:bg-primary text-white text-[9px] font-black uppercase tracking-widest px-3 py-2 border-none cursor-pointer flex items-center gap-1"
                                  >
                                    <span className="material-symbols-outlined text-xs">qr_code_scanner</span> Quét nhận phòng
                                  </button>
                                  <button 
                                    onClick={() => handleDirectCheckInOut(bk, 'Checked In')}
                                    className="bg-slate-200 hover:bg-slate-300 text-slate-800 text-[9px] font-black uppercase tracking-widest px-3 py-2 border-none cursor-pointer"
                                  >
                                    Check-in nhanh
                                  </button>
                                </>
                              )}
                              {isCheckedIn && (
                                <button 
                                  onClick={() => handleDirectCheckInOut(bk, 'Checked Out')}
                                  className="bg-primary hover:brightness-110 text-white text-[9px] font-black uppercase tracking-widest px-4 py-2 border-none cursor-pointer"
                                >
                                  Trả phòng (Check-out)
                                </button>
                              )}
                            </div>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            </div>
          )}

          {/* QUẢN LÝ LOẠI PHÒNG (MANAGER ROOM TYPES CRUD) */}
          {activeTab === 'rooms' && isManager && (
            <div className="space-y-6 animate-scale-in">
              <div className="flex justify-between items-end">
                <div>
                  <h3 className="font-headline-lg text-lg text-primary uppercase italic tracking-wider m-0">QUẢN LÝ LOẠI PHÒNG</h3>
                  <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest mt-1">Cấu hình thông tin danh mục phòng ngủ và đơn giá</p>
                </div>

                <button 
                  onClick={handleOpenAddRoom}
                  className="bg-primary text-white font-black text-xs uppercase tracking-widest px-6 py-3 border-none cursor-pointer hover:brightness-110 active:scale-98 transition-all flex items-center gap-1.5"
                >
                  <span className="material-symbols-outlined text-sm font-bold">add</span> Thêm loại phòng
                </button>
              </div>

              {/* Room Form Overlay / Collapsible form */}
              {isRoomFormOpen && (
                <div className="bg-white border border-outline-variant p-6 md:p-8 shadow-xl animate-scale-in">
                  <h4 className="text-xs font-black text-slate-900 uppercase tracking-widest border-b border-slate-100 pb-3 mb-6">
                    {editingRoom ? `CHỈNH SỬA LOẠI PHÒNG: ${editingRoom.name}` : 'THÊM MỚI LOẠI PHÒNG'}
                  </h4>

                  <form onSubmit={handleRoomSubmit} className="space-y-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div className="space-y-2">
                        <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Tên loại phòng</label>
                        <input 
                          type="text"
                          value={roomFormData.name}
                          onChange={(e) => setRoomFormData(prev => ({ ...prev, name: e.target.value }))}
                          placeholder="E.g., Suite River View"
                          className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary"
                        />
                      </div>

                      <div className="space-y-2">
                        <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Đơn giá cơ bản (VND / đêm)</label>
                        <input 
                          type="number"
                          value={roomFormData.basePrice}
                          onChange={(e) => setRoomFormData(prev => ({ ...prev, basePrice: e.target.value }))}
                          placeholder="E.g., 5000000"
                          className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary"
                        />
                      </div>

                      <div className="space-y-2">
                        <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Sức chứa người lớn / phòng</label>
                        <input 
                          type="number"
                          value={roomFormData.adultCapacity}
                          onChange={(e) => setRoomFormData(prev => ({ ...prev, adultCapacity: e.target.value }))}
                          className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary"
                        />
                      </div>

                      <div className="space-y-2">
                        <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Sức chứa trẻ em / phòng</label>
                        <input 
                          type="number"
                          value={roomFormData.childCapacity}
                          onChange={(e) => setRoomFormData(prev => ({ ...prev, childCapacity: e.target.value }))}
                          className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary"
                        />
                      </div>

                      <div className="space-y-2">
                        <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Loại giường</label>
                        <input 
                          type="text"
                          value={roomFormData.bedType}
                          onChange={(e) => setRoomFormData(prev => ({ ...prev, bedType: e.target.value }))}
                          className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary"
                        />
                      </div>

                      <div className="space-y-2">
                        <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Diện tích phòng (m²)</label>
                        <input 
                          type="number"
                          value={roomFormData.roomSize}
                          onChange={(e) => setRoomFormData(prev => ({ ...prev, roomSize: e.target.value }))}
                          className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary"
                        />
                      </div>

                      <div className="space-y-2">
                        <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Trạng thái hoạt động</label>
                        <select 
                          value={roomFormData.status}
                          onChange={(e) => setRoomFormData(prev => ({ ...prev, status: e.target.value }))}
                          className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary"
                        >
                          <option value="Active">Active</option>
                          <option value="Inactive">Inactive</option>
                        </select>
                      </div>

                      <div className="space-y-2">
                        <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Hình ảnh phòng</label>
                        <input 
                          type="file" 
                          onChange={handleFileChange}
                          className="w-full text-xs text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-none file:border-0 file:text-[10px] file:font-black file:uppercase file:bg-slate-100 file:text-slate-800 hover:file:bg-slate-200 cursor-pointer"
                        />
                      </div>
                    </div>

                    <div className="space-y-2">
                      <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Mô tả loại phòng</label>
                      <textarea 
                        rows="3"
                        value={roomFormData.description}
                        onChange={(e) => setRoomFormData(prev => ({ ...prev, description: e.target.value }))}
                        className="w-full bg-transparent border border-slate-200 p-3 font-bold text-sm outline-none focus:border-primary resize-none"
                      />
                    </div>

                    <div className="flex gap-4">
                      <button 
                        type="submit" 
                        disabled={isSubmittingRoom}
                        className="bg-primary text-on-primary font-bold px-8 py-3.5 uppercase text-xs tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-1.5"
                      >
                        {isSubmittingRoom ? 'Đang lưu...' : 'Lưu lại'}
                      </button>
                      <button 
                        type="button" 
                        onClick={() => setIsRoomFormOpen(false)}
                        className="bg-slate-200 text-slate-800 font-bold px-8 py-3.5 uppercase text-xs tracking-widest hover:bg-slate-350 cursor-pointer border-none"
                      >
                        Hủy bỏ
                      </button>
                    </div>
                  </form>
                </div>
              )}

              {/* Room Types table */}
              <div className="bg-white border border-outline-variant shadow-md overflow-hidden">
                <table className="w-full border-collapse text-xs font-bold text-slate-700">
                  <thead>
                    <tr className="bg-slate-50 border-b border-slate-200 text-[9px] font-black uppercase tracking-wider text-slate-500 text-left">
                      <th className="p-4">Hình ảnh</th>
                      <th className="p-4">Tên loại phòng</th>
                      <th className="p-4">Giá / đêm</th>
                      <th className="p-4">Giường / Diện tích</th>
                      <th className="p-4">Sức chứa tối đa</th>
                      <th className="p-4">Trạng thái</th>
                      <th className="p-4 text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-150">
                    {roomTypes.map((room) => (
                      <tr key={room.id} className="hover:bg-slate-50/50 transition-colors">
                        <td className="p-4">
                          <img 
                            src={room.primaryImageUrl || 'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=100&q=80'} 
                            alt={room.name}
                            className="w-16 h-12 object-cover border border-slate-200"
                          />
                        </td>
                        <td className="p-4">
                          <span className="text-slate-900 font-black uppercase text-sm block">{room.name}</span>
                          <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block mt-0.5">ID: {room.id}</span>
                        </td>
                        <td className="p-4 text-primary font-black">
                          {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(room.basePrice || room.baseprice || 0)}
                        </td>
                        <td className="p-4 font-semibold text-slate-500">
                          {room.bedType || 'King Bed'} <br />
                          {room.roomSize || room.roomsize || 35} m²
                        </td>
                        <td className="p-4">
                          {room.adultCapacity || room.adultcapacity || 2} NL • {room.childCapacity || room.childcapacity || 1} TE
                        </td>
                        <td className="p-4">
                          <span className={`px-2 py-0.5 text-[8px] font-black uppercase tracking-widest ${
                            room.status === 'Active' || !room.status ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                          }`}>
                            {room.status || 'Active'}
                          </span>
                        </td>
                        <td className="p-4 text-right">
                          <div className="flex justify-end gap-2">
                            <button 
                              onClick={() => handleOpenEditRoom(room)}
                              className="bg-slate-100 hover:bg-slate-200 text-slate-700 text-[10px] font-bold uppercase px-3 py-1.5 border-none cursor-pointer flex items-center gap-1"
                            >
                              Sửa
                            </button>
                            <button 
                              onClick={() => handleDeleteRoom(room.id, room.name)}
                              className="bg-red-50 hover:bg-red-100 text-red-600 text-[10px] font-bold uppercase px-3 py-1.5 border-none cursor-pointer flex items-center gap-1"
                            >
                              Xóa
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* BÁO CÁO DOANH THU (MANAGER REPORTS) */}
          {activeTab === 'reports' && isManager && (
            <div className="space-y-6 animate-scale-in">
              <div>
                <h3 className="font-headline-lg text-lg text-primary uppercase italic tracking-wider m-0">BÁO CÁO DOANH THU & HIỆU SUẤT</h3>
                <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest mt-1">Phân tích tình hình tài chính và doanh thu đặt phòng</p>
              </div>

              {/* Revenue grid charts */}
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                
                {/* Column chart with raw CSS */}
                <div className="lg:col-span-2 bg-white border border-outline-variant p-6 shadow-md">
                  <h4 className="text-xs font-black text-slate-900 uppercase tracking-widest border-b border-slate-100 pb-3 mb-6">
                    Biểu đồ doanh thu 6 tháng gần nhất
                  </h4>

                  <div className="h-64 flex items-end justify-between gap-4 pt-4 px-2">
                    {[
                      { month: 'T1', rev: '98M', val: 65 },
                      { month: 'T2', rev: '120M', val: 80 },
                      { month: 'T3', rev: '85M', val: 55 },
                      { month: 'T4', rev: '110M', val: 72 },
                      { month: 'T5', rev: '147M', val: 95 },
                      { month: 'T6', rev: '160M', val: 100 },
                    ].map((item, idx) => (
                      <div key={idx} className="flex-1 flex flex-col items-center gap-2">
                        <span className="text-[9px] text-primary font-black">{item.rev}</span>
                        <div 
                          className="w-full bg-primary hover:bg-slate-900 transition-all duration-300 shadow-md"
                          style={{ height: `${item.val * 1.8}px` }}
                        />
                        <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">{item.month}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Occupancy card */}
                <div className="bg-slate-950 text-white border border-slate-900 p-6 shadow-md flex flex-col justify-between">
                  <div>
                    <h4 className="text-xs font-black uppercase tracking-widest text-primary border-b border-slate-900 pb-3 mb-4">Hiệu suất phòng nghỉ</h4>
                    <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider leading-relaxed">Phân phối tỷ lệ lấp đầy giữa các phân khúc khách hàng.</p>
                  </div>

                  <div className="space-y-4 py-4">
                    <div>
                      <div className="flex justify-between text-[10px] font-bold uppercase tracking-wider mb-1">
                        <span>Đoàn / Sự kiện (Group)</span>
                        <span>42%</span>
                      </div>
                      <div className="w-full h-1.5 bg-slate-800"><div className="bg-primary h-full" style={{ width: '42%' }}></div></div>
                    </div>

                    <div>
                      <div className="flex justify-between text-[10px] font-bold uppercase tracking-wider mb-1">
                        <span>Đặt phòng trực tuyến (Online)</span>
                        <span>48%</span>
                      </div>
                      <div className="w-full h-1.5 bg-slate-800"><div className="bg-blue-600 h-full" style={{ width: '48%' }}></div></div>
                    </div>

                    <div>
                      <div className="flex justify-between text-[10px] font-bold uppercase tracking-wider mb-1">
                        <span>Đặt phòng trực tiếp (Walk-in)</span>
                        <span>10%</span>
                      </div>
                      <div className="w-full h-1.5 bg-slate-800"><div className="bg-green-600 h-full" style={{ width: '10%' }}></div></div>
                    </div>
                  </div>

                  <div className="text-[9px] text-slate-500 font-bold uppercase tracking-wider border-t border-slate-900 pt-3">
                    Dữ liệu được làm mới tự động mỗi 12 giờ.
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* HỒ SƠ CÁ NHÂN (PROFILE SETTINGS) */}
          {activeTab === 'settings' && (
            <div className="bg-white border border-outline-variant p-6 md:p-8 shadow-md animate-scale-in">
              <Profile 
                initialProfile={currentUser}
                onProfileUpdate={(updated) => {
                  setCurrentUser(updated);
                  localStorage.setItem('user', JSON.stringify(updated));
                }}
                showToast={showToast}
              />
            </div>
          )}
        </main>
      </div>

      {selectedBooking && (
        <div className="fixed inset-0 bg-slate-950/70 z-50 flex items-center justify-center p-4">
          <div className="bg-white border border-slate-200 max-w-md w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl animate-scale-in text-slate-800 text-left">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">CHI TIẾT ĐƠN ĐẶT PHÒNG</span>
                <h4 className="text-sm font-black uppercase text-slate-900 m-0 mt-0.5">{selectedBooking.bookingReference}</h4>
              </div>
              <button 
                onClick={() => setSelectedBooking(null)} 
                className="text-slate-400 hover:text-slate-800 border-none bg-transparent cursor-pointer flex items-center"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <div className="space-y-4 text-xs font-bold text-slate-700">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Khách hàng</span>
                  <span className="text-slate-950 font-black uppercase block mt-0.5">{selectedBooking.guestName}</span>
                </div>
                <div>
                  <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Số điện thoại</span>
                  <span className="text-slate-950 font-semibold block mt-0.5">{selectedBooking.guestPhone || 'N/A'}</span>
                </div>
              </div>

              <div>
                <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Email</span>
                <span className="text-slate-950 font-semibold block mt-0.5">{selectedBooking.email || 'N/A'}</span>
              </div>

              <div className="border-t border-slate-100 pt-3 grid grid-cols-2 gap-4">
                <div>
                  <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Loại phòng</span>
                  <span className="text-slate-950 font-black block mt-0.5">{selectedBooking.roomType}</span>
                </div>
                <div>
                  <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Số lượng & Khách</span>
                  <span className="text-slate-950 font-black block mt-0.5">
                    {selectedBooking.quantity} phòng ({selectedBooking.numberOfAdults} NL {selectedBooking.numberOfChildren > 0 ? `• ${selectedBooking.numberOfChildren} TE` : ''})
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Ngày nhận phòng</span>
                  <span className="text-slate-950 font-semibold block mt-0.5">{selectedBooking.checkInDate}</span>
                </div>
                <div>
                  <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Ngày trả phòng</span>
                  <span className="text-slate-950 font-semibold block mt-0.5">{selectedBooking.checkOutDate} ({selectedBooking.nights} đêm)</span>
                </div>
              </div>

              <div className="border-t border-slate-100 pt-3 grid grid-cols-2 gap-4">
                <div>
                  <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Hình thức Check-in</span>
                  <span className="text-primary font-black uppercase block mt-0.5">{selectedBooking.checkInMethod === 'Face Recognition' ? 'FaceID eKYC' : selectedBooking.checkInMethod}</span>
                </div>
                <div>
                  <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Trạng thái</span>
                  <span className={`inline-block mt-0.5 px-2 py-0.5 text-[8px] font-black uppercase tracking-widest ${
                    selectedBooking.status === 'Checked In' ? 'bg-blue-100 text-blue-700' : selectedBooking.status === 'Confirmed' ? 'bg-green-100 text-green-700' : 'bg-slate-150 text-slate-500'
                  }`}>
                    {selectedBooking.status}
                  </span>
                </div>
              </div>

              <div>
                <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Yêu cầu đặc biệt</span>
                <p className="text-slate-950 font-medium italic m-0 mt-0.5 bg-slate-50 p-2.5 border border-slate-100 leading-relaxed rounded-sm">
                  {selectedBooking.specialRequests || 'Không có yêu cầu đặc biệt.'}
                </p>
              </div>

              <div className="border-t border-slate-100 pt-4 flex justify-between items-center">
                <div>
                  <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Tổng chi phí</span>
                  <span className="text-base font-black text-primary block mt-0.5">
                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedBooking.totalAmount)}
                  </span>
                </div>
                <button 
                  onClick={() => setSelectedBooking(null)}
                  className="bg-slate-900 hover:bg-slate-850 text-white text-[10px] font-black uppercase tracking-widest py-3 px-6 border-none cursor-pointer"
                >
                  Đóng
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
