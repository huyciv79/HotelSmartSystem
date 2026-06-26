import { useState, useEffect } from 'react';
import { getRoomTypes } from '../services/roomService';
import { createRoomType, updateRoomType, deleteRoomType, getRooms, updateRoom } from '../services/roomManagementService';
import { 
  getBookingHistory, 
  getAllBookings, 
  checkInBooking, 
  checkOutBooking,
  createWalkInBooking,
  getInvoiceDetails
} from '../services/bookingService';
import {
  getPendingRefundRequests,
  approveRefundRequest,
  rejectRefundRequest
} from '../services/refundService';
  getStatementPdf
} from '../services/bookingService';
import { getUserProfile } from '../services/userService';
import Profile from './Profile';
import { useToast, ToastContainer } from '../components/Toast';

// Staff dashboard sub-components
import StaffSidebar from '../components/staff/StaffSidebar';
import StaffHeader from '../components/staff/StaffHeader';
import StaffOverview from '../components/staff/StaffOverview';
import RoomTypesManager from '../components/staff/RoomTypesManager';
import RoomsManager from '../components/staff/RoomsManager';
import RevenueReports from '../components/staff/RevenueReports';
import FaceCheckInStation from '../components/staff/FaceCheckInStation';

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

  const isManager = String(currentUser.role || '').toLowerCase() === 'manager';

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

  // Individual Rooms states for Manager CRUD
  const [roomsList, setRoomsList] = useState([]);
  const [roomsTotalPages, setRoomsTotalPages] = useState(1);
  const [roomsCurrentPage, setRoomsCurrentPage] = useState(0);
  const [roomsFilterStatus, setRoomsFilterStatus] = useState('');
  const [roomsFilterType, setRoomsFilterType] = useState('');
  const [roomsSearchQuery, setRoomsSearchQuery] = useState('');
  const [isRoomEditOpen, setIsRoomEditOpen] = useState(false);
  const [editingRoomItem, setEditingRoomItem] = useState(null);
  const [roomEditFormData, setRoomEditFormData] = useState({
    roomNumber: '',
    floorNumber: '',
    roomTypeId: '',
    status: 'Available'
  });
  const [isSubmittingRoomEdit, setIsSubmittingRoomEdit] = useState(false);

  // Walk-in booking modal states
  const [isWalkInModalOpen, setIsWalkInModalOpen] = useState(false);
  const [isSubmittingWalkIn, setIsSubmittingWalkIn] = useState(false);
  const [walkInFormData, setWalkInFormData] = useState({
    roomTypeId: '',
    checkInDate: '',
    checkOutDate: '',
    numberOfAdults: 1,
    numberOfChildren: 0,
    specialRequests: '',
    customerFullname: '',
    customerEmail: '',
    customerPhonenumber: '',
    paidAmount: '',
    paymentMethod: 'Cash'
  });

  // Invoice modal states
  const [isInvoiceModalOpen, setIsInvoiceModalOpen] = useState(false);
  const [invoiceLoading, setInvoiceLoading] = useState(false);
  const [invoiceData, setInvoiceData] = useState(null);

  // Refund states
  const [pendingRefunds, setPendingRefunds] = useState([]);
  const [refundsLoading, setRefundsLoading] = useState(false);
  const [isApproveRefundOpen, setIsApproveRefundOpen] = useState(false);
  const [isRejectRefundOpen, setIsRejectRefundOpen] = useState(false);
  const [selectedRefund, setSelectedRefund] = useState(null);
  const [refundOverrideAmount, setRefundOverrideAmount] = useState('');
  const [refundRejectionReason, setRefundRejectionReason] = useState('');
  const [isSubmittingRefundAction, setIsSubmittingRefundAction] = useState(false);

  // Simulated Camera / FaceID Scanning Modal
  const [scanningBooking, setScanningBooking] = useState(null);
  const [scanType, setScanType] = useState(null); // 'face' | 'qr'
  const [isScanning, setIsScanning] = useState(false);
  const [selectedBooking, setSelectedBooking] = useState(null);

  const fetchRealRooms = async (page = 0) => {
    try {
      const criteria = {
        page: page,
        size: 10,
        keyword: roomsSearchQuery,
        status: roomsFilterStatus,
        roomTypeId: roomsFilterType
      };
      const response = await getRooms(criteria);
      if (response && response.success && response.data) {
        setRoomsList(response.data.content || []);
        setRoomsTotalPages(response.data.totalPages || 1);
        setRoomsCurrentPage(response.data.page !== undefined ? response.data.page : (response.data.number || 0));
      }
    } catch (err) {
      showToast('Không thể tải danh sách phòng', 'error');
    }
  };

  const fetchPendingRefunds = async () => {
    setRefundsLoading(true);
    try {
      const response = await getPendingRefundRequests();
      if (response && response.success) {
        setPendingRefunds(response.data || []);
      }
    } catch (err) {
      console.error('Lỗi khi tải yêu cầu hoàn tiền:', err);
      showToast('Không thể tải danh sách yêu cầu hoàn tiền', 'error');
    } finally {
      setRefundsLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'refunds') {
      fetchPendingRefunds();
    }
  }, [activeTab]);



  const handleWalkInSubmit = async (e) => {
    e.preventDefault();
    if (!walkInFormData.roomTypeId || !walkInFormData.checkInDate || !walkInFormData.checkOutDate || !walkInFormData.customerFullname || !walkInFormData.customerEmail || !walkInFormData.customerPhonenumber) {
      showToast('Vui lòng điền đầy đủ các thông tin bắt buộc', 'warning');
      return;
    }
    setIsSubmittingWalkIn(true);
    try {
      const response = await createWalkInBooking({
        ...walkInFormData,
        roomTypeId: parseInt(walkInFormData.roomTypeId),
        quantity: parseInt(walkInFormData.quantity || 1),
        numberOfAdults: parseInt(walkInFormData.numberOfAdults || 1),
        numberOfChildren: parseInt(walkInFormData.numberOfChildren || 0),
        paidAmount: walkInFormData.paidAmount ? parseFloat(walkInFormData.paidAmount) : 0,
      });
      if (response && response.success) {
        showToast('Đặt phòng Walk-in và nhận phòng thành công!', 'success');
        setIsWalkInModalOpen(false);
        fetchRealBookings();
      }
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Lỗi khi thực hiện đặt phòng Walk-in.', 'error');
    } finally {
      setIsSubmittingWalkIn(false);
    }
  };

  const handleViewInvoice = async (bookingId) => {
    setInvoiceLoading(true);
    setIsInvoiceModalOpen(true);
    setInvoiceData(null);
    try {
      const response = await getInvoiceDetails(bookingId);
      if (response && response.success) {
        setInvoiceData(response.data);
      }
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Không thể tải chi tiết hóa đơn.', 'error');
      setIsInvoiceModalOpen(false);
    } finally {
      setInvoiceLoading(false);
    }
  };



  const handleApproveRefundSubmit = async (e) => {
    e.preventDefault();
    setIsSubmittingRefundAction(true);
    try {
      const amount = refundOverrideAmount ? parseFloat(refundOverrideAmount) : null;
      const response = await approveRefundRequest(selectedRefund.requestId, amount);
      if (response && response.success) {
        showToast('Phê duyệt hoàn tiền thành công!', 'success');
        setIsApproveRefundOpen(false);
        setSelectedRefund(null);
        setRefundOverrideAmount('');
        fetchPendingRefunds();
        fetchRealBookings();
      }
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Lỗi khi phê duyệt hoàn tiền.', 'error');
    } finally {
      setIsSubmittingRefundAction(false);
    }
  };

  const handleRejectRefundSubmit = async (e) => {
    e.preventDefault();
    if (!refundRejectionReason.trim()) {
      showToast('Vui lòng nhập lý do từ chối', 'warning');
      return;
    }
    setIsSubmittingRefundAction(true);
    try {
      const response = await rejectRefundRequest(selectedRefund.requestId, refundRejectionReason);
      if (response && response.success) {
        showToast('Đã từ chối yêu cầu hoàn tiền thành công!', 'success');
        setIsRejectRefundOpen(false);
        setSelectedRefund(null);
        setRefundRejectionReason('');
        fetchPendingRefunds();
      }
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Lỗi khi từ chối hoàn tiền.', 'error');
    } finally {
      setIsSubmittingRefundAction(false);
    }
  };

  useEffect(() => {
    let socket = null;
    
    const connectWebSocket = () => {
      try {
        socket = new WebSocket('ws://localhost:8080/ws/websocket');
        
        socket.onopen = () => {
          socket.send("CONNECT\naccept-version:1.1,1.2\n\n\x00");
        };
        
        socket.onmessage = (event) => {
          const raw = event.data;
          if (raw.startsWith("CONNECTED")) {
            socket.send("SUBSCRIBE\nid:sub-frontend\ndestination:/topic/room-status\n\n\x00");
            console.log('WebSocket STOMP connected and subscribed.');
          } else if (raw.includes("/topic/room-status")) {
            const bodyStart = raw.indexOf('{');
            const bodyEnd = raw.lastIndexOf('}');
            if (bodyStart !== -1 && bodyEnd !== -1) {
              try {
                const bodyStr = raw.substring(bodyStart, bodyEnd + 1);
                const data = JSON.parse(bodyStr);
                
                showToast(`Phòng ${data.roomNumber} đã chuyển sang trạng thái: ${data.status}`, 'info');
                fetchRealRooms(roomsCurrentPage);
                fetchRealBookings();
              } catch (ex) {
                console.error('Lỗi phân giải tin nhắn WebSocket:', ex);
              }
            }
          }
        };
        
        socket.onerror = (err) => {
          console.error('Lỗi kết nối WebSocket:', err);
        };
        
        socket.onclose = () => {
          console.log('Kết nối WebSocket đã đóng. Đang thử kết nối lại sau 5s...');
          setTimeout(connectWebSocket, 5000);
        };
      } catch (e) {
        console.error('Không thể tạo kết nối WebSocket:', e);
      }
    };
    
    connectWebSocket();
    
    return () => {
      if (socket) {
        socket.onclose = null;
        socket.close();
      }
    };
  }, []);

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const token = localStorage.getItem('accessToken');
        if (token) {
          const response = await getUserProfile();
          if (response && response.data) {
            setCurrentUser(response.data);
            localStorage.setItem('user', JSON.stringify(response.data));
          }
        }
      } catch (err) {
        console.error('Lỗi khi tải thông tin hồ sơ nhân viên:', err);
      }
    };

    fetchProfile();
    fetchRoomTypes();
    fetchRealBookings();
    if (isManager) {
      fetchRealRooms(0);
    }
  }, [isManager]);

  useEffect(() => {
    if (isManager && activeTab === 'rooms-list') {
      fetchRealRooms(0);
    }
  }, [roomsFilterStatus, roomsFilterType, activeTab]);

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
        try {
          const response = await getBookingHistory();
          if (response && response.data) {
            allBookings = [...response.data];
          }
        } catch (e2) { }
        try {
          const localCreated = JSON.parse(localStorage.getItem('hotel_all_bookings') || '[]');
          localCreated.forEach(localBk => {
            if (!allBookings.some(b => b.bookingId === localBk.bookingId)) {
              allBookings.push(localBk);
            }
          });
        } catch (e3) { }
      }

      if (allBookings.length > 0) {
        const realMapped = allBookings.map(bk => {
          const localStatus = localStorage.getItem(`booking_status_${bk.bookingId}`) || bk.status;
          return {
            source: 'backend',
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
            roomNumber: bk.roomNumber || '',
            roomPassword: bk.roomPassword || '',
            roomKeyStatus: bk.roomKeyStatus || '',
            roomKeyExpiresAt: bk.roomKeyExpiresAt || null,
            roomAccesses: bk.roomAccesses || [],
            ekycIdentity: bk.ekycIdentity || null,
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
      const response = await getRoomTypes('all');
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

  const mergeBookingResult = (booking, bookingResult, status) => ({
    ...booking,
    status,
    roomNumber: bookingResult.roomNumber ?? booking.roomNumber,
    roomPassword: bookingResult.roomPassword ?? '',
    roomKeyStatus: bookingResult.roomKeyStatus ?? booking.roomKeyStatus,
    roomKeyExpiresAt: bookingResult.roomKeyExpiresAt ?? booking.roomKeyExpiresAt,
    roomAccesses: bookingResult.roomAccesses ?? booking.roomAccesses ?? [],
    ekycIdentity: bookingResult.ekycIdentity ?? booking.ekycIdentity ?? null,
  });

  const completeScannerAction = async () => {
    const isCheckIn = scanningBooking.status === 'Confirmed';
    const nextStatus = isCheckIn ? 'Checked In' : 'Checked Out';

    try {
      const response = isCheckIn
        ? await checkInBooking(scanningBooking.id)
        : await checkOutBooking(scanningBooking.id);
      const bookingResult = response?.data || {};

      localStorage.setItem(`booking_status_${scanningBooking.id}`, nextStatus);
      if (nextStatus === 'Checked In') {
        localStorage.setItem(`booking_actualcheckin_${scanningBooking.id}`, new Date().toISOString());
      } else if (nextStatus === 'Checked Out') {
        localStorage.setItem(`booking_actualcheckout_${scanningBooking.id}`, new Date().toISOString());
      }

      setBookings(prev => prev.map(bk =>
        bk.id === scanningBooking.id
          ? mergeBookingResult(bk, bookingResult, nextStatus)
          : bk
      ));
      setSelectedBooking(current =>
        current?.id === scanningBooking.id
          ? mergeBookingResult(current, bookingResult, nextStatus)
          : current
      );

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
      const response = isCheckIn
        ? await checkInBooking(booking.id)
        : await checkOutBooking(booking.id);
      const bookingResult = response?.data || {};

      localStorage.setItem(`booking_status_${booking.id}`, targetStatus);
      if (targetStatus === 'Checked In') {
        localStorage.setItem(`booking_actualcheckin_${booking.id}`, new Date().toISOString());
      } else if (targetStatus === 'Checked Out') {
        localStorage.setItem(`booking_actualcheckout_${booking.id}`, new Date().toISOString());
      }

      setBookings(prev => prev.map(bk =>
        bk.id === booking.id
          ? mergeBookingResult(bk, bookingResult, targetStatus)
          : bk
      ));
      setSelectedBooking(current =>
        current?.id === booking.id
          ? mergeBookingResult(current, bookingResult, targetStatus)
          : current
      );
      showToast(`Đã chuyển trạng thái sang ${targetStatus === 'Checked In' ? 'ĐÃ NHẬN PHÒNG' : 'ĐÃ TRẢ PHÒNG'}!`, 'success');
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Có lỗi xảy ra khi cập nhật trạng thái.', 'error');
    }
  };

  const handleFaceCheckInCompleted = (bookingId, bookingResult) => {
    setBookings(prev => prev.map(bk =>
      bk.id === bookingId
        ? mergeBookingResult(bk, bookingResult, 'Checked In')
        : bk
    ));
    setSelectedBooking(current =>
      current?.id === bookingId
        ? mergeBookingResult(current, bookingResult, 'Checked In')
        : current
    );
    localStorage.setItem(`booking_status_${bookingId}`, 'Checked In');
    localStorage.setItem(`booking_actualcheckin_${bookingId}`, new Date().toISOString());
  };

  const handleDownloadPdf = async (bookingId, bookingRef) => {
    try {
      const blob = await getStatementPdf(bookingId);
      const url = window.URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `Statement_${bookingRef}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      showToast('Đã tải xuống Statement PDF thành công!', 'success');
    } catch (err) {
      console.error(err);
      showToast('Không thể kết xuất PDF bảng sao kê.', 'error');
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
    setActiveTab('rooms');
  };

  const handleOpenEditRoom = (room) => {
    setEditingRoom(room);
    setRoomFormData({
      name: room.name || '',
      basePrice: room.basePrice || room.baseprice || '',
      adultCapacity: String(room.adultCapacity || room.adultcapacity || '2'),
      childCapacity: String(room.childCapacity || room.childcapacity || '1'),
      bedType: room.bedType || 'Giường Đôi King Size',
      roomSize: String(room.area || room.roomSize || room.roomsize || '35'),
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
      formData.append('area', roomFormData.roomSize);
      formData.append('status', roomFormData.status);
      formData.append('description', roomFormData.description);
      if (roomImage) {
        formData.append('images', roomImage);
        if (editingRoom) {
          formData.append('replaceImages', 'true');
        }
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

  const handleOpenEditRoomItem = (room) => {
    setEditingRoomItem(room);
    setRoomEditFormData({
      roomNumber: room.roomnumber || room.roomNumber || '',
      floorNumber: String(room.floornumber !== undefined ? room.floornumber : (room.floorNumber !== undefined ? room.floorNumber : '')),
      roomTypeId: String(room.roomtypeid || room.roomTypeId || (room.roomType && room.roomType.roomTypeId) || ''),
      status: room.status || 'Available',
      adminPasscode: room.adminpasscode || room.adminPasscode || ''
    });
    setIsRoomEditOpen(true);
  };

  const handleRoomEditSubmit = async (e) => {
    e.preventDefault();
    if (!roomEditFormData.roomNumber || !roomEditFormData.floorNumber || !roomEditFormData.roomTypeId) {
      showToast('Vui lòng điền đầy đủ thông tin phòng', 'warning');
      return;
    }

    setIsSubmittingRoomEdit(true);
    try {
      const payload = {
        roomNumber: roomEditFormData.roomNumber,
        floorNumber: parseInt(roomEditFormData.floorNumber),
        roomTypeId: parseInt(roomEditFormData.roomTypeId),
        status: roomEditFormData.status,
        adminPasscode: roomEditFormData.adminPasscode
      };
      await updateRoom(editingRoomItem.roomId || editingRoomItem.id, payload);
      showToast(`Đã cập nhật phòng ${roomEditFormData.roomNumber} thành công!`, 'success');
      setIsRoomEditOpen(false);
      fetchRealRooms(roomsCurrentPage);
    } catch (err) {
      console.error('Lỗi khi cập nhật phòng:', err);
      showToast(err.response?.data?.message || 'Có lỗi xảy ra khi cập nhật phòng.', 'error');
    } finally {
      setIsSubmittingRoomEdit(false);
    }
  };

  return (
    <>
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />

      <div className="min-h-screen bg-[#070708] flex font-['Montserrat'] text-slate-100 text-left pt-0">

        {/* SIDEBAR */}
        <StaffSidebar
          activeTab={activeTab}
          setActiveTab={setActiveTab}
          currentUser={currentUser}
          isManager={isManager}
          handleLogout={handleLogout}
        />

        {/* MAIN PANEL CONTENT */}
        <div className="ml-72 flex-1 min-h-screen flex flex-col">

          {/* HEADER */}
          <StaffHeader
            currentUser={currentUser}
            isManager={isManager}
            searchQuery={searchQuery}
            setSearchQuery={setSearchQuery}
          />

          <main className="p-10 flex flex-col gap-8 flex-1 overflow-y-auto">

            {/* TỔNG QUAN (OVERVIEW) */}
            {activeTab === 'overview' && (
              <StaffOverview
                bookings={bookings}
                roomTypes={roomTypes}
                currentUser={currentUser}
                isManager={isManager}
                setActiveTab={setActiveTab}
                handleOpenAddRoom={handleOpenAddRoom}
                setSelectedBooking={setSelectedBooking}
                startScanner={startScanner}
                handleDirectCheckInOut={handleDirectCheckInOut}
                handleOpenWalkIn={() => setIsWalkInModalOpen(true)}
              />
            )}

            {/* VẬN HÀNH SẢNH / CHECK-IN / CHECK-OUT */}
            {activeTab === 'operations' && (
              <div className="space-y-6 animate-scale-in">
                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-end gap-4 border-b border-neutral-900 pb-4">
                  <div>
                    <h3 className="text-white font-black text-base uppercase tracking-wider m-0">VẬN HÀNH SẢNH & CHECK-IN</h3>
                    <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Tìm kiếm khách hàng làm thủ tục nhận phòng hoặc trả phòng</p>
                  </div>
                </div>

                {/* Simulated Scanning Modal popup */}
                {isScanning && scanningBooking && (
                  <div className="bg-[#0f0f12] border-2 border-primary p-6 text-white text-center shadow-2xl relative animate-scale-in">
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
                <div className="bg-[#0f0f12] border border-neutral-900 shadow-md overflow-hidden">
                  <div className="p-4 border-b border-neutral-900 bg-neutral-950/20 text-[10px] font-black uppercase text-slate-400 tracking-wider">
                    Danh sách đặt phòng cần xử lý trong ngày
                  </div>

                  <div className="divide-y divide-neutral-900/60">
                    {filteredBookings.length === 0 ? (
                      <div className="py-12 text-center text-slate-500 font-bold text-xs uppercase tracking-widest">
                        Không tìm thấy lịch trình đặt phòng nào phù hợp
                      </div>
                    ) : (
                      filteredBookings.map((bk) => {
                        const isConfirmed = bk.status === 'Confirmed';
                        const isCheckedIn = bk.status === 'Checked In';
                        const usesFaceId =
                          bk.checkInMethod === 'Face Recognition' ||
                          bk.checkInMethod === 'FaceID';

                        return (
                          <div key={bk.id} className="p-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-6 hover:bg-white/5 transition-colors">
                            <div className="space-y-1">
                              <span className="inline-block px-2 py-0.5 text-[8px] font-black tracking-widest text-slate-400 bg-neutral-900 border border-neutral-800 uppercase mb-1">
                                {bk.bookingType && bk.bookingType.toLowerCase() === 'group' ? 'ĐOÀN (GROUP)' : 'ĐƠN LẺ'}
                              </span>
                              <h5 className="text-sm font-black text-white uppercase tracking-wider m-0">
                                {bk.guestName}
                              </h5>
                              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">
                                Mã: {bk.bookingReference} • {bk.roomType}
                              </p>
                              <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">
                                Ngày lưu trú: {bk.checkInDate} đến {bk.checkOutDate} ({bk.nights} đêm)
                              </p>
                            </div>

                            <div className="flex flex-wrap items-center gap-4 self-stretch md:self-auto justify-between md:justify-end">
                              <div className="text-left md:text-right shrink-0">
                                <span className="text-[8px] text-slate-500 font-bold uppercase tracking-wider block">Check-in bằng:</span>
                                <span className="text-[10px] text-primary font-black uppercase tracking-wider">{bk.checkInMethod === 'Face Recognition' || bk.checkInMethod === 'FaceID' ? 'FaceID eKYC' : bk.checkInMethod}</span>
                                <span className="text-xs font-black text-white block mt-1">
                                  {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(bk.totalAmount)}
                                </span>
                              </div>

                              <span className={`px-2.5 py-1 text-[8px] font-black uppercase tracking-widest ${
                                isCheckedIn 
                                  ? 'bg-blue-900/30 text-blue-400 border border-blue-900/50' 
                                  : isConfirmed 
                                  ? 'bg-green-900/30 text-green-400 border border-green-900/50' 
                                  : 'bg-neutral-800 text-neutral-400 border border-neutral-700/50'
                              }`}>
                                {bk.status}
                              </span>

                              <div className="flex gap-2">
                                <button
                                  onClick={() => setSelectedBooking(bk)}
                                  className="bg-neutral-900 border border-neutral-800 text-white text-[9px] font-black uppercase tracking-widest px-3 py-2 cursor-pointer flex items-center gap-1 hover:bg-neutral-800"
                                >
                                  <span className="material-symbols-outlined text-xs">info</span> Chi tiết
                                </button>
                                <button
                                  onClick={() => handleViewInvoice(bk.id)}
                                  className="bg-neutral-900 border border-neutral-800 text-white text-[9px] font-black uppercase tracking-widest px-3 py-2 cursor-pointer flex items-center gap-1 hover:bg-neutral-800"
                                >
                                  <span className="material-symbols-outlined text-xs">receipt_long</span> Hóa đơn
                                </button>

                                  onClick={() => handleDownloadPdf(bk.id, bk.bookingReference)}
                                  className="bg-neutral-900 border border-neutral-800 text-white text-[9px] font-black uppercase tracking-widest px-3 py-2 cursor-pointer flex items-center gap-1 hover:bg-neutral-800"
                                >
                                  <span className="material-symbols-outlined text-xs">download</span> Tải PDF
                                </button>
                                {isConfirmed && (
                                  <>
                                    <button
                                      onClick={() => {
                                        if (usesFaceId && isManager) {
                                          setActiveTab('face-check-in');
                                          return;
                                        }
                                        startScanner(bk, 'qr');
                                      }}
                                      disabled={usesFaceId && !isManager}
                                      className="bg-primary hover:brightness-110 disabled:bg-neutral-800 disabled:text-slate-500 disabled:cursor-not-allowed text-white text-[9px] font-black uppercase tracking-widest px-3 py-2 border-none cursor-pointer flex items-center gap-1"
                                    >
                                      <span className="material-symbols-outlined text-xs">
                                        {usesFaceId ? 'face' : 'qr_code_scanner'}
                                      </span>
                                      {usesFaceId
                                        ? isManager
                                          ? 'FaceID tại sảnh'
                                          : 'Cần Manager'
                                        : 'Quét nhận phòng'}
                                    </button>
                                    {!usesFaceId && (
                                      <button
                                        onClick={() => handleDirectCheckInOut(bk, 'Checked In')}
                                        className="bg-neutral-900 border border-neutral-800 text-white hover:bg-neutral-800 text-[9px] font-black uppercase tracking-widest px-3 py-2 cursor-pointer"
                                      >
                                        Check-in nhanh
                                      </button>
                                    )}
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
            {activeTab === 'face-check-in' && isManager && (
              <FaceCheckInStation
                bookings={bookings}
                showToast={showToast}
                onCheckInCompleted={handleFaceCheckInCompleted}
              />
            )}

            {activeTab === 'rooms' && isManager && (
              <RoomTypesManager
                roomTypes={roomTypes}
                isRoomFormOpen={isRoomFormOpen}
                setIsRoomFormOpen={setIsRoomFormOpen}
                editingRoom={editingRoom}
                roomFormData={roomFormData}
                setRoomFormData={setRoomFormData}
                handleFileChange={handleFileChange}
                handleRoomSubmit={handleRoomSubmit}
                handleOpenAddRoom={handleOpenAddRoom}
                handleOpenEditRoom={handleOpenEditRoom}
                handleDeleteRoom={handleDeleteRoom}
                isSubmittingRoom={isSubmittingRoom}
              />
            )}

            {/* QUẢN LÝ DANH SÁCH PHÒNG (MANAGER ROOMS LIST CRUD) */}
            {activeTab === 'rooms-list' && isManager && (
              <RoomsManager
                roomsList={roomsList}
                roomsTotalPages={roomsTotalPages}
                roomsCurrentPage={roomsCurrentPage}
                roomsFilterStatus={roomsFilterStatus}
                setRoomsFilterStatus={setRoomsFilterStatus}
                roomsFilterType={roomsFilterType}
                setRoomsFilterType={setRoomsFilterType}
                roomsSearchQuery={roomsSearchQuery}
                setRoomsSearchQuery={setRoomsSearchQuery}
                fetchRealRooms={fetchRealRooms}
                handleOpenEditRoomItem={handleOpenEditRoomItem}
                roomTypes={roomTypes}
              />
            )}

            {/* BÁO CÁO DOANH THU (MANAGER REPORTS) */}
            {activeTab === 'reports' && isManager && (
              <RevenueReports />
            )}

            {/* YÊU CẦU HOÀN TIỀN (REFUNDS MANAGEMENT) */}
            {activeTab === 'refunds' && (
              <div className="space-y-6 animate-scale-in text-left">
                <div>
                  <h3 className="text-white font-black text-base uppercase tracking-wider m-0">QUẢN LÝ YÊU CẦU HOÀN TIỀN</h3>
                  <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Danh sách yêu cầu hoàn tiền đang chờ phê duyệt từ khách hàng</p>
                </div>

                <div className="bg-[#0f0f12] border border-neutral-900 shadow-md overflow-hidden">
                  <div className="p-4 border-b border-neutral-900 bg-neutral-950/20 text-[10px] font-black uppercase text-slate-400 tracking-wider">
                    Yêu cầu chờ xử lý ({pendingRefunds.length})
                  </div>

                  {refundsLoading ? (
                    <div className="py-12 text-center text-slate-500 font-bold text-xs uppercase tracking-widest">
                      Đang tải danh sách...
                    </div>
                  ) : pendingRefunds.length === 0 ? (
                    <div className="py-12 text-center text-slate-500 font-bold text-xs uppercase tracking-widest">
                      Không có yêu cầu hoàn tiền nào đang chờ xử lý
                    </div>
                  ) : (
                    <div className="overflow-x-auto">
                      <table className="w-full border-collapse text-left text-xs text-slate-400">
                        <thead>
                          <tr className="border-b border-neutral-900 text-[9px] font-black uppercase text-slate-500 tracking-wider">
                            <th className="p-4">Mã Đơn / Ngày gửi</th>
                            <th className="p-4">Lý do hoàn tiền</th>
                            <th className="p-4">Số tiền ban đầu / Ước tính hoàn</th>
                            <th className="p-4 text-right">Thao tác</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-neutral-900/60">
                          {pendingRefunds.map((req) => (
                            <tr key={req.requestId} className="hover:bg-white/5 transition-colors">
                              <td className="p-4">
                                <span className="text-white font-black uppercase block">{req.bookingReference}</span>
                                <span className="text-[9px] text-slate-500 block mt-0.5">
                                  Gửi lúc: {new Date(req.createdAt).toLocaleString('vi-VN')}
                                </span>
                              </td>
                              <td className="p-4 font-medium text-slate-300">
                                {req.description}
                              </td>
                              <td className="p-4">
                                <div className="text-[10px] text-slate-500">Đã thanh toán: <strong className="text-white">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(parseFloat(req.oldValue))}</strong></div>
                                <div className="text-[10px] text-primary font-bold">Hoàn dự kiến: {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(parseFloat(req.newValue))}</div>
                              </td>
                              <td className="p-4 text-right">
                                <div className="flex gap-2 justify-end">
                                  <button
                                    onClick={() => {
                                      setSelectedRefund(req);
                                      setRefundOverrideAmount(req.newValue);
                                      setIsApproveRefundOpen(true);
                                    }}
                                    className="bg-green-600 hover:bg-green-700 text-white text-[9.5px] font-black uppercase tracking-widest px-3 py-2 cursor-pointer border-none"
                                  >
                                    Phê duyệt
                                  </button>
                                  <button
                                    onClick={() => {
                                      setSelectedRefund(req);
                                      setRefundRejectionReason('');
                                      setIsRejectRefundOpen(true);
                                    }}
                                    className="bg-rose-600 hover:bg-rose-700 text-white text-[9.5px] font-black uppercase tracking-widest px-3 py-2 cursor-pointer border-none"
                                  >
                                    Từ chối
                                  </button>
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* HỒ SƠ CÁ NHÂN (PROFILE SETTINGS) */}
            {activeTab === 'settings' && (
              <div className="animate-scale-in">
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
      </div>

      {/* Booking Detail Dialog */}
      {selectedBooking && (
        <div className="fixed inset-0 bg-neutral-950/80 z-50 flex items-center justify-center p-4">
          <div className="bg-[#0f0f12] border border-neutral-900 max-w-md w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl animate-scale-in text-white text-left font-['Montserrat']">
            <div className="flex justify-between items-center border-b border-neutral-850 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">CHI TIẾT ĐƠN ĐẶT PHÒNG</span>
                <h4 className="text-sm font-black uppercase text-white m-0 mt-0.5">{selectedBooking.bookingReference}</h4>
              </div>
              <button
                onClick={() => setSelectedBooking(null)}
                className="text-slate-400 hover:text-white border-none bg-transparent cursor-pointer flex items-center"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <div className="space-y-4 text-xs font-bold text-slate-300">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Khách hàng</span>
                  <span className="text-white font-black uppercase block mt-0.5">{selectedBooking.guestName}</span>
                </div>
                <div>
                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Số điện thoại</span>
                  <span className="text-white font-semibold block mt-0.5">{selectedBooking.guestPhone || 'N/A'}</span>
                </div>
              </div>

              <div>
                <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Email</span>
                <span className="text-white font-semibold block mt-0.5">{selectedBooking.email || 'N/A'}</span>
              </div>

              <div className="border-t border-neutral-900 pt-3 grid grid-cols-2 gap-4">
                <div>
                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Loại phòng</span>
                  <span className="text-white font-black block mt-0.5">{selectedBooking.roomType}</span>
                </div>
                <div>
                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Số lượng & Khách</span>
                  <span className="text-white font-black block mt-0.5">
                    {selectedBooking.quantity} phòng ({selectedBooking.numberOfAdults} NL {selectedBooking.numberOfChildren > 0 ? `• ${selectedBooking.numberOfChildren} TE` : ''})
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Ngày nhận phòng</span>
                  <span className="text-white font-semibold block mt-0.5">{selectedBooking.checkInDate}</span>
                </div>
                <div>
                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Ngày trả phòng</span>
                  <span className="text-white font-semibold block mt-0.5">{selectedBooking.checkOutDate} ({selectedBooking.nights} đêm)</span>
                </div>
              </div>

              <div className="border-t border-neutral-900 pt-3 grid grid-cols-2 gap-4">
                <div>
                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Hình thức Check-in</span>
                  <span className="text-primary font-black uppercase block mt-0.5">{selectedBooking.checkInMethod === 'Face Recognition' || selectedBooking.checkInMethod === 'FaceID' ? 'FaceID eKYC' : selectedBooking.checkInMethod}</span>
                </div>
                <div>
                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Trạng thái</span>
                  <span className={`inline-block mt-0.5 px-2 py-0.5 text-[8px] font-black uppercase tracking-widest ${
                    selectedBooking.status === 'Checked In' 
                      ? 'bg-blue-900/30 text-blue-400 border border-blue-900/50' 
                      : selectedBooking.status === 'Confirmed' 
                      ? 'bg-green-900/30 text-green-400 border border-green-900/50' 
                      : 'bg-neutral-800 text-neutral-400 border border-neutral-700/50'
                  }`}>
                    {selectedBooking.status}
                  </span>
                </div>
              </div>

              {selectedBooking.roomAccesses?.length > 0 && (
                <div className="border-t border-neutral-900 pt-3">
                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">
                    Phòng và mật khẩu đã cấp
                  </span>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-2">
                    {selectedBooking.roomAccesses.map((access) => (
                      <div
                        key={access.roomId || access.roomNumber}
                        className="bg-neutral-950 border border-neutral-850 p-3 flex items-center justify-between"
                      >
                        <div>
                          <span className="block text-white font-black">
                            Phòng {access.roomNumber}
                          </span>
                          <span className="block text-[9px] text-slate-600 mt-0.5">
                            Tầng {access.floorNumber ?? 'N/A'}
                          </span>
                        </div>
                        <span className="font-mono text-primary font-black tracking-[0.18em]">
                          {access.roomPassword || 'Đã khóa'}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <div>
                <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Yêu cầu đặc biệt</span>
                <p className="text-white font-medium italic m-0 mt-0.5 bg-neutral-950 p-2.5 border border-neutral-850 leading-relaxed rounded-sm">
                  {selectedBooking.specialRequests || 'Không có yêu cầu đặc biệt.'}
                </p>
              </div>

              <div className="border-t border-neutral-900 pt-4 flex justify-between items-center">
                <div>
                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Tổng chi phí</span>
                  <span className="text-base font-black text-primary block mt-0.5">
                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedBooking.totalAmount)}
                  </span>
                </div>
                <button
                  onClick={() => setSelectedBooking(null)}
                  className="bg-neutral-900 border border-neutral-800 hover:bg-neutral-800 text-white text-[10px] font-black uppercase tracking-widest py-3 px-6 border-none cursor-pointer"
                >
                  Đóng
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Room Edit Dialog */}
      {isRoomEditOpen && (
        <div className="fixed inset-0 bg-neutral-950/80 z-50 flex items-center justify-center p-4">
          <div className="bg-[#0f0f12] border border-neutral-900 max-w-md w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl animate-scale-in text-white text-left font-['Montserrat']">
            <div className="flex justify-between items-center border-b border-neutral-850 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">CẬP NHẬT PHÒNG VẬT LÝ</span>
                <h4 className="text-sm font-black uppercase text-white m-0 mt-0.5">Phòng {editingRoomItem?.roomNumber}</h4>
              </div>
              <button
                onClick={() => setIsRoomEditOpen(false)}
                className="text-slate-400 hover:text-white border-none bg-transparent cursor-pointer flex items-center"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <form onSubmit={handleRoomEditSubmit} className="space-y-5">
              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Số phòng</label>
                <input
                  type="text"
                  value={roomEditFormData.roomNumber}
                  onChange={(e) => setRoomEditFormData(prev => ({ ...prev, roomNumber: e.target.value }))}
                  className="w-full bg-transparent border-b border-neutral-850 py-2 font-bold text-xs outline-none text-white focus:border-primary"
                  required
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Tầng</label>
                <input
                  type="number"
                  value={roomEditFormData.floorNumber}
                  onChange={(e) => setRoomEditFormData(prev => ({ ...prev, floorNumber: e.target.value }))}
                  className="w-full bg-transparent border-b border-neutral-850 py-2 font-bold text-xs outline-none text-white focus:border-primary"
                  required
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Hạng phòng (Loại phòng)</label>
                <select
                  value={roomEditFormData.roomTypeId}
                  onChange={(e) => setRoomEditFormData(prev => ({ ...prev, roomTypeId: e.target.value }))}
                  className="w-full bg-transparent border-b border-neutral-850 py-2 font-bold text-xs outline-none text-white focus:border-primary [&>option]:bg-neutral-900 [&>option]:text-white"
                  required
                >
                  <option value="">Chọn loại phòng</option>
                  {roomTypes.map(type => (
                    <option key={type.id} value={type.id}>{type.name}</option>
                  ))}
                </select>
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Mật mã phòng</label>
                <input
                  type="text"
                  value={roomEditFormData.adminPasscode || ''}
                  onChange={(e) => setRoomEditFormData(prev => ({ ...prev, adminPasscode: e.target.value }))}
                  placeholder="Nhập mật mã phòng..."
                  className="w-full bg-transparent border-b border-neutral-850 py-2 font-bold text-xs outline-none text-white focus:border-primary placeholder:text-slate-600"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Trạng thái phòng</label>
                <select
                  value={roomEditFormData.status}
                  onChange={(e) => setRoomEditFormData(prev => ({ ...prev, status: e.target.value }))}
                  className="w-full bg-transparent border-b border-neutral-850 py-2 font-bold text-xs outline-none text-white focus:border-primary [&>option]:bg-neutral-900 [&>option]:text-white"
                  required
                >
                  <option value="Available">Available</option>
                  <option value="Occupied">Occupied</option>
                  <option value="Cleaning">Cleaning</option>
                  <option value="Maintenance">Maintenance</option>
                </select>
              </div>

              <div className="flex gap-4 pt-4 border-t border-neutral-900">
                <button
                  type="submit"
                  disabled={isSubmittingRoomEdit}
                  className="bg-primary text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 flex-1"
                >
                  {isSubmittingRoomEdit ? 'Đang lưu...' : 'Lưu thay đổi'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsRoomEditOpen(false)}
                  className="bg-neutral-900 hover:bg-neutral-855 border border-neutral-800 text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest cursor-pointer flex-1"
                >
                  Hủy bỏ
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* WALK-IN BOOKING FORM MODAL */}
      {isWalkInModalOpen && (
        <div className="fixed inset-0 bg-neutral-950/80 z-50 flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-[#0f0f12] border border-neutral-900 max-w-2xl w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl animate-scale-in text-white text-left font-['Montserrat'] my-8">
            <div className="flex justify-between items-center border-b border-neutral-850 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">HỆ THỐNG LỄ TÂN</span>
                <h4 className="text-sm font-black uppercase text-white m-0 mt-0.5">Đặt phòng Walk-in trực tiếp</h4>
              </div>
              <button
                onClick={() => setIsWalkInModalOpen(false)}
                className="text-slate-400 hover:text-white border-none bg-transparent cursor-pointer flex items-center"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <form onSubmit={handleWalkInSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* Room type selection */}
                <div className="space-y-2">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Loại phòng *</label>
                  <select
                    value={walkInFormData.roomTypeId}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, roomTypeId: e.target.value }))}
                    className="w-full bg-transparent border-b border-neutral-850 py-2 font-bold text-xs outline-none text-white focus:border-primary [&>option]:bg-neutral-900 [&>option]:text-white"
                    required
                  >
                    <option value="">Chọn loại phòng</option>
                    {roomTypes.map(type => (
                      <option key={type.id} value={type.id}>{type.name} - {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(type.basePrice || type.baseprice)}</option>
                    ))}
                  </select>
                </div>

                <div className="space-y-2">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Số lượng phòng *</label>
                  <input
                    type="number"
                    min="1"
                    value={walkInFormData.quantity || 1}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, quantity: e.target.value }))}
                    className="w-full bg-transparent border-b border-neutral-850 py-2 font-bold text-xs outline-none text-white focus:border-primary"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Ngày nhận phòng (Check-in) *</label>
                  <input
                    type="date"
                    value={walkInFormData.checkInDate}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, checkInDate: e.target.value }))}
                    className="w-full bg-transparent border-b border-neutral-855 py-2 font-bold text-xs outline-none text-white focus:border-primary [&::-webkit-calendar-picker-indicator]:filter [&::-webkit-calendar-picker-indicator]:invert"
                    required
                  />
                </div>

                <div className="space-y-2">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Ngày trả phòng (Check-out) *</label>
                  <input
                    type="date"
                    value={walkInFormData.checkOutDate}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, checkOutDate: e.target.value }))}
                    className="w-full bg-transparent border-b border-neutral-855 py-2 font-bold text-xs outline-none text-white focus:border-primary [&::-webkit-calendar-picker-indicator]:filter [&::-webkit-calendar-picker-indicator]:invert"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Người lớn *</label>
                  <input
                    type="number"
                    min="1"
                    value={walkInFormData.numberOfAdults || 1}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, numberOfAdults: e.target.value }))}
                    className="w-full bg-transparent border-b border-neutral-855 py-2 font-bold text-xs outline-none text-white focus:border-primary"
                    required
                  />
                </div>

                <div className="space-y-2">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Trẻ em</label>
                  <input
                    type="number"
                    min="0"
                    value={walkInFormData.numberOfChildren || 0}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, numberOfChildren: e.target.value }))}
                    className="w-full bg-transparent border-b border-neutral-855 py-2 font-bold text-xs outline-none text-white focus:border-primary"
                  />
                </div>
              </div>

              <div className="border-t border-neutral-900/60 pt-4">
                <span className="text-[10px] font-black tracking-widest text-primary uppercase block mb-3">Thông tin khách hàng</span>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div className="space-y-2">
                    <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Họ và tên *</label>
                    <input
                      type="text"
                      value={walkInFormData.customerFullname}
                      onChange={(e) => setWalkInFormData(prev => ({ ...prev, customerFullname: e.target.value }))}
                      placeholder="Nguyễn Văn A"
                      className="w-full bg-transparent border-b border-neutral-855 py-2 font-bold text-xs outline-none text-white focus:border-primary"
                      required
                    />
                  </div>

                  <div className="space-y-2">
                    <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Email *</label>
                    <input
                      type="email"
                      value={walkInFormData.customerEmail}
                      onChange={(e) => setWalkInFormData(prev => ({ ...prev, customerEmail: e.target.value }))}
                      placeholder="email@example.com"
                      className="w-full bg-transparent border-b border-neutral-855 py-2 font-bold text-xs outline-none text-white focus:border-primary"
                      required
                    />
                  </div>

                  <div className="space-y-2">
                    <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Số điện thoại *</label>
                    <input
                      type="tel"
                      value={walkInFormData.customerPhonenumber}
                      onChange={(e) => setWalkInFormData(prev => ({ ...prev, customerPhonenumber: e.target.value }))}
                      placeholder="0901234567"
                      className="w-full bg-transparent border-b border-neutral-855 py-2 font-bold text-xs outline-none text-white focus:border-primary"
                      required
                    />
                  </div>
                </div>
              </div>

              <div className="border-t border-neutral-900/60 pt-4 grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Số tiền đóng trước (đ)</label>
                  <input
                    type="number"
                    value={walkInFormData.paidAmount}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, paidAmount: e.target.value }))}
                    placeholder="0"
                    className="w-full bg-transparent border-b border-neutral-855 py-2 font-bold text-xs outline-none text-white focus:border-primary"
                  />
                </div>

                <div className="space-y-2">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Phương thức thanh toán</label>
                  <select
                    value={walkInFormData.paymentMethod}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, paymentMethod: e.target.value }))}
                    className="w-full bg-transparent border-b border-neutral-855 py-2 font-bold text-xs outline-none text-white focus:border-primary [&>option]:bg-neutral-900 [&>option]:text-white"
                  >
                    <option value="Cash">Tiền mặt (Cash)</option>
                    <option value="Card">Thẻ (Card)</option>
                    <option value="Transfer">Chuyển khoản (Transfer)</option>
                  </select>
                </div>
              </div>

              <div className="space-y-2">
                <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Yêu cầu đặc biệt</label>
                <textarea
                  value={walkInFormData.specialRequests}
                  onChange={(e) => setWalkInFormData(prev => ({ ...prev, specialRequests: e.target.value }))}
                  placeholder="Yêu cầu khác..."
                  rows="2"
                  className="w-full bg-transparent border border-neutral-855 p-2 font-bold text-xs outline-none text-white focus:border-primary resize-none"
                />
              </div>

              <div className="flex gap-4 pt-4 border-t border-neutral-900">
                <button
                  type="submit"
                  disabled={isSubmittingWalkIn}
                  className="bg-primary text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest hover:brightness-110 transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 flex-1"
                >
                  {isSubmittingWalkIn ? 'Đang đặt phòng...' : 'Xác nhận Đặt & Check-in'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsWalkInModalOpen(false)}
                  className="bg-neutral-900 hover:bg-neutral-850 border border-neutral-800 text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest cursor-pointer flex-1"
                >
                  Hủy bỏ
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* INVOICE DETAIL MODAL */}
      {isInvoiceModalOpen && (
        <div className="fixed inset-0 bg-neutral-950/80 z-50 flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-[#0f0f12] border border-neutral-900 max-w-2xl w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl animate-scale-in text-white text-left font-['Montserrat'] my-8">
            <div className="flex justify-between items-center border-b border-neutral-855 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">HÓA ĐƠN CHI TIẾT</span>
                <h4 className="text-sm font-black uppercase text-white m-0 mt-0.5">
                  {invoiceLoading ? 'Đang tải...' : `Mã đơn: ${invoiceData?.bookingReference}`}
                </h4>
              </div>
              <button
                onClick={() => setIsInvoiceModalOpen(false)}
                className="text-slate-400 hover:text-white border-none bg-transparent cursor-pointer flex items-center"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            {invoiceLoading ? (
              <div className="py-12 flex flex-col items-center justify-center gap-3">
                <span className="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin"></span>
                <span className="text-xs uppercase tracking-widest font-bold text-slate-500">Đang tải hóa đơn...</span>
              </div>
            ) : invoiceData ? (
              <div className="space-y-6 text-xs text-slate-300">
                {/* Guest info */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 border-b border-neutral-900 pb-4">
                  <div>
                    <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Khách hàng</span>
                    <strong className="text-white text-sm block mt-1 uppercase font-black">{invoiceData.customerName}</strong>
                    <span className="block mt-0.5">{invoiceData.customerEmail} • {invoiceData.customerPhone}</span>
                  </div>
                  <div>
                    <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Thời gian lưu trú</span>
                    <strong className="text-white block mt-1">{invoiceData.checkInDate} đến {invoiceData.checkOutDate}</strong>
                    <span className="block mt-0.5">Tổng cộng: {invoiceData.nights} đêm • Hạng phòng: {invoiceData.roomTypeName}</span>
                  </div>
                </div>

                {/* Calculation details */}
                <div className="space-y-3">
                  <div className="flex justify-between border-b border-neutral-900 pb-2">
                    <span className="font-bold uppercase tracking-wider text-slate-400">Diễn giải dịch vụ</span>
                    <span className="font-bold uppercase tracking-wider text-slate-400">Thành tiền</span>
                  </div>

                  {/* Room Charge */}
                  <div className="flex justify-between text-white font-medium">
                    <div>
                      <span>Tiền phòng ({invoiceData.quantity || 1} phòng x {invoiceData.nights} đêm)</span>
                      <span className="block text-[10px] text-slate-500 font-semibold">Đơn giá: {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(invoiceData.roomRate)}</span>
                    </div>
                    <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(invoiceData.roomTotal)}</span>
                  </div>

                  {/* Service charges */}
                  {invoiceData.services && invoiceData.services.length > 0 && (
                    <div className="space-y-2 border-t border-neutral-900 pt-3">
                      <span className="block text-[9px] font-black text-slate-500 uppercase tracking-widest">Dịch vụ phụ trội</span>
                      {invoiceData.services.map((svc, idx) => (
                        <div key={idx} className="flex justify-between font-medium">
                          <div>
                            <span>{svc.serviceName} (x{svc.quantity})</span>
                            {svc.note && <span className="block text-[10px] text-slate-500 italic font-semibold">{svc.note}</span>}
                          </div>
                          <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(svc.totalPrice)}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Summary values */}
                <div className="border-t border-neutral-900 pt-4 space-y-2">
                  <div className="flex justify-between text-slate-400 font-bold">
                    <span>Tổng chưa thuế:</span>
                    <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(parseFloat(invoiceData.roomTotal) + parseFloat(invoiceData.serviceTotal || 0))}</span>
                  </div>
                  <div className="flex justify-between text-slate-400 font-bold">
                    <span>Thuế VAT (10%):</span>
                    <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(invoiceData.taxAmount)}</span>
                  </div>
                  {parseFloat(invoiceData.discountAmount || 0) > 0 && (
                    <div className="flex justify-between text-rose-400 font-bold">
                      <span>Giảm giá:</span>
                      <span>-{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(invoiceData.discountAmount)}</span>
                    </div>
                  )}
                  <div className="flex justify-between border-t border-neutral-900 pt-3 text-sm">
                    <span className="font-black text-white uppercase tracking-wider">TỔNG CỘNG HÓA ĐƠN:</span>
                    <span className="font-black text-primary text-base">
                      {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(invoiceData.finalAmount)}
                    </span>
                  </div>
                  <div className="flex justify-between text-green-400 font-bold">
                    <span>Đã thanh toán:</span>
                    <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(invoiceData.paidAmount)}</span>
                  </div>
                  <div className="flex justify-between border-t border-neutral-900 pt-2 text-xs font-black text-white">
                    <span>CÒN LẠI PHẢI THANH TOÁN (DUE):</span>
                    <span className={parseFloat(invoiceData.dueAmount) > 0 ? "text-rose-500" : "text-green-500"}>
                      {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(invoiceData.dueAmount)}
                    </span>
                  </div>
                </div>

                {/* Payments breakdown */}
                {invoiceData.payments && invoiceData.payments.length > 0 && (
                  <div className="border-t border-neutral-900 pt-4">
                    <span className="block text-[9px] font-black text-slate-500 uppercase tracking-widest mb-2">Lịch sử thanh toán</span>
                    <div className="space-y-2">
                      {invoiceData.payments.map((pmt, idx) => (
                        <div key={idx} className="bg-neutral-950 p-3 border border-neutral-855 flex justify-between items-center text-[10px]">
                          <div>
                            <span className="block font-black text-white uppercase">{pmt.paymentType === 'Advance' ? 'Đặt cọc' : 'Thanh toán'} • {pmt.paymentMethod}</span>
                            <span className="block text-[9px] text-slate-500 mt-0.5">Mã giao dịch: {pmt.transactionCode || 'N/A'} • Ngày: {new Date(pmt.paymentDate).toLocaleString('vi-VN')}</span>
                          </div>
                          <div className="text-right">
                            <span className="block font-black text-primary">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(pmt.amount)}</span>
                            {parseFloat(pmt.refundedAmount || 0) > 0 && (
                              <span className="block text-[9px] text-rose-500 font-semibold">Đã hoàn: -{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(pmt.refundedAmount)}</span>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <div className="flex justify-end pt-4 border-t border-neutral-900">
                  <button
                    onClick={() => setIsInvoiceModalOpen(false)}
                    className="bg-neutral-900 hover:bg-neutral-850 border border-neutral-800 text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest cursor-pointer"
                  >
                    Đóng
                  </button>
                </div>
              </div>
            ) : (
              <div className="py-12 text-center text-rose-500 font-bold uppercase tracking-wider">
                Không thể tải chi tiết hóa đơn
              </div>
            )}
          </div>
        </div>
      )}

      {/* APPROVE REFUND MODAL */}
      {isApproveRefundOpen && selectedRefund && (
        <div className="fixed inset-0 bg-neutral-950/80 z-50 flex items-center justify-center p-4">
          <div className="bg-[#0f0f12] border border-neutral-900 max-w-md w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl animate-scale-in text-white text-left font-['Montserrat']">
            <div className="flex justify-between items-center border-b border-neutral-850 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">PHÊ DUYỆT HOÀN TIỀN</span>
                <h4 className="text-sm font-black uppercase text-white m-0 mt-0.5">{selectedRefund.bookingReference}</h4>
              </div>
              <button
                onClick={() => setIsApproveRefundOpen(false)}
                className="text-slate-400 hover:text-white border-none bg-transparent cursor-pointer flex items-center"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <form onSubmit={handleApproveRefundSubmit} className="space-y-4">
              <div className="bg-neutral-950 border border-neutral-855 p-4 space-y-2 text-xs">
                <div className="flex justify-between gap-4">
                  <span className="text-slate-500 uppercase font-bold text-[9px] shrink-0">Lý do từ khách:</span>
                  <span className="text-white font-bold text-right">{selectedRefund.description}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500 uppercase font-bold text-[9px]">Số tiền ban đầu:</span>
                  <span className="text-white font-bold">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(parseFloat(selectedRefund.oldValue))}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500 uppercase font-bold text-[9px]">Hoàn tiền dự kiến:</span>
                  <span className="text-primary font-black">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(parseFloat(selectedRefund.newValue))}</span>
                </div>
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">
                  Số tiền hoàn trả thực tế (đ):
                </label>
                <input
                  type="number"
                  value={refundOverrideAmount}
                  onChange={(e) => setRefundOverrideAmount(e.target.value)}
                  placeholder="Nhập số tiền hoàn trả..."
                  className="w-full bg-transparent border-b border-neutral-855 py-2 font-bold text-xs outline-none text-white focus:border-primary"
                  required
                />
                <span className="text-[9px] text-slate-500 uppercase block leading-relaxed">
                  *Để trống hoặc nhập số tiền khác để ghi đè. Không được vượt quá số tiền ban đầu.
                </span>
              </div>

              <div className="flex gap-4 pt-4 border-t border-neutral-900">
                <button
                  type="submit"
                  disabled={isSubmittingRefundAction}
                  className="bg-green-600 text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest hover:bg-green-700 transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 flex-1"
                >
                  {isSubmittingRefundAction ? 'Đang xử lý...' : 'Xác nhận duyệt'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsApproveRefundOpen(false)}
                  className="bg-neutral-900 hover:bg-neutral-850 border border-neutral-800 text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest cursor-pointer flex-1"
                >
                  Hủy bỏ
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* REJECT REFUND MODAL */}
      {isRejectRefundOpen && selectedRefund && (
        <div className="fixed inset-0 bg-neutral-950/80 z-50 flex items-center justify-center p-4">
          <div className="bg-[#0f0f12] border border-neutral-900 max-w-md w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl animate-scale-in text-white text-left font-['Montserrat']">
            <div className="flex justify-between items-center border-b border-neutral-850 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">TỪ CHỐI HOÀN TIỀN</span>
                <h4 className="text-sm font-black uppercase text-white m-0 mt-0.5">{selectedRefund.bookingReference}</h4>
              </div>
              <button
                onClick={() => setIsRejectRefundOpen(false)}
                className="text-slate-400 hover:text-white border-none bg-transparent cursor-pointer flex items-center"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <form onSubmit={handleRejectRefundSubmit} className="space-y-4">
              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">
                  Lý do từ chối yêu cầu:
                </label>
                <textarea
                  rows="4"
                  value={refundRejectionReason}
                  onChange={(e) => setRefundRejectionReason(e.target.value)}
                  placeholder="Vui lòng cung cấp lý do từ chối..."
                  className="w-full bg-transparent border border-neutral-855 p-3 font-bold text-xs outline-none text-white focus:border-primary resize-none"
                  required
                />
              </div>

              <div className="flex gap-4 pt-4 border-t border-neutral-900">
                <button
                  type="submit"
                  disabled={isSubmittingRefundAction}
                  className="bg-[#e11d48] text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest hover:brightness-110 transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 flex-1"
                >
                  {isSubmittingRefundAction ? 'Đang xử lý...' : 'Xác nhận từ chối'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsRejectRefundOpen(false)}
                  className="bg-neutral-900 hover:bg-neutral-850 border border-neutral-855 text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest cursor-pointer flex-1"
                >
                  Hủy bỏ
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
