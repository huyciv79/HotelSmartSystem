import { useState, useEffect } from 'react';
import { getRoomTypes } from '../services/roomService';
import { createRoomType, updateRoomType, deleteRoomType, getRooms, updateRoom } from '../services/roomManagementService';
import { 
  getBookingHistory, 
  getAllBookings, 
  checkInBooking, 
  checkOutBooking,
  createWalkInBooking,
  getInvoiceDetails,
  getStatementPdf,
  processManualPayment,
  getAllServices,
  addServiceToBooking,
  cancelBooking
} from '../services/bookingService';
import {
  getPendingRefundRequests,
  approveRefundRequest,
  rejectRefundRequest
} from '../services/refundService';
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
import BookingsTable from '../components/staff/BookingsTable';

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
  const [bookings, setBookings] = useState([]);
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

  // Custom Confirm Dialog state
  const [confirmDialog, setConfirmDialog] = useState({
    isOpen: false,
    title: '',
    message: '',
    onConfirm: null
  });

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
  // Add Service States
  const [isAddServiceModalOpen, setIsAddServiceModalOpen] = useState(false);
  const [selectedServiceBooking, setSelectedServiceBooking] = useState(null);
  const [servicesList, setServicesList] = useState([]);
  const [selectedServiceId, setSelectedServiceId] = useState('');
  const [serviceQuantity, setServiceQuantity] = useState(1);
  const [serviceNote, setServiceNote] = useState('');
  const [isSubmittingService, setIsSubmittingService] = useState(false);
  const [invoiceData, setInvoiceData] = useState(null);

  const [inlineServiceId, setInlineServiceId] = useState('');
  const [inlineServiceQuantity, setInlineServiceQuantity] = useState(1);
  const [inlineServiceNote, setInlineServiceNote] = useState('');
  const [isSubmittingInlineService, setIsSubmittingInlineService] = useState(false);

  // Manual counter payment states
  const [manualPaymentMethod, setManualPaymentMethod] = useState('Cash');
  const [manualPaymentAmount, setManualPaymentAmount] = useState('');
  const [manualPaymentNotes, setManualPaymentNotes] = useState('');
  const [isSubmittingManualPayment, setIsSubmittingManualPayment] = useState(false);
  const [operationsSubTab, setOperationsSubTab] = useState('checkin');

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
  const [faceCheckInBookingId, setFaceCheckInBookingId] = useState('');

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
    } else if (activeTab === 'overview' || activeTab === 'operations') {
      fetchRealBookings();
    }
  }, [activeTab]);

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
    setManualPaymentMethod('Cash');
    setManualPaymentNotes('');
    setInlineServiceId('');
    setInlineServiceQuantity(1);
    setInlineServiceNote('');
    try {
      const svcRes = await getAllServices();
      if (svcRes && svcRes.data) {
        setServicesList(svcRes.data);
        if (svcRes.data.length > 0) {
          setInlineServiceId(svcRes.data[0].id.toString());
        }
      }
    } catch (e) {
      console.error('Lỗi khi tải danh sách dịch vụ:', e);
    }
    try {
      const response = await getInvoiceDetails(bookingId);
      if (response && response.success) {
        setInvoiceData(response.data);
        setManualPaymentAmount(response.data.dueAmount || '');
      }
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Không thể tải chi tiết hóa đơn.', 'error');
      setIsInvoiceModalOpen(false);
    } finally {
      setInvoiceLoading(false);
    }
  };

  const handleManualPaymentSubmit = async (e) => {
    e.preventDefault();
    if (!manualPaymentAmount || parseFloat(manualPaymentAmount) <= 0) {
      showToast('Vui lòng nhập số tiền thanh toán hợp lệ lớn hơn 0', 'warning');
      return;
    }
    setIsSubmittingManualPayment(true);
    try {
      const response = await processManualPayment({
        bookingId: invoiceData.bookingId,
        amount: parseFloat(manualPaymentAmount),
        paymentMethod: manualPaymentMethod,
        paymentType: 'Booking Payment',
        notes: manualPaymentNotes || `Ghi nhận thanh toán tại quầy (${manualPaymentMethod})`
      });

      if (response && response.success) {
        showToast('Ghi nhận thanh toán tại quầy thành công!', 'success');
        // Refresh invoice detail modal
        const freshInvoice = await getInvoiceDetails(invoiceData.bookingId);
        if (freshInvoice && freshInvoice.success) {
          setInvoiceData(freshInvoice.data);
          setManualPaymentAmount(freshInvoice.data.dueAmount || '');
        }
        // Refresh booking list
        fetchRealBookings();
      }
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Lỗi khi ghi nhận thanh toán tại quầy.', 'error');
    } finally {
      setIsSubmittingManualPayment(false);
    }
  };

  const handleInlineServiceSubmit = async (e) => {
    e.preventDefault();
    if (!inlineServiceId) {
      showToast('Vui lòng chọn dịch vụ', 'warning');
      return;
    }
    setIsSubmittingInlineService(true);
    try {
      await addServiceToBooking(
        invoiceData.bookingId,
        parseInt(inlineServiceId),
        inlineServiceQuantity,
        inlineServiceNote
      );
      showToast('Đã thêm dịch vụ thành công!', 'success');
      setInlineServiceNote('');
      setInlineServiceQuantity(1);
      
      // Refresh invoice modal
      const freshInvoice = await getInvoiceDetails(invoiceData.bookingId);
      if (freshInvoice && freshInvoice.success) {
        setInvoiceData(freshInvoice.data);
        setManualPaymentAmount(freshInvoice.data.dueAmount || '');
      }
      
      fetchRealBookings();
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Không thể thêm dịch vụ phụ thu.', 'error');
    } finally {
      setIsSubmittingInlineService(false);
    }
  };

  const handleOpenAddService = async (booking) => {
    setSelectedServiceBooking(booking);
    setSelectedServiceId('');
    setServiceQuantity(1);
    setServiceNote('');
    setIsAddServiceModalOpen(true);
    
    try {
      const response = await getAllServices();
      if (response && response.data) {
        setServicesList(response.data);
        if (response.data.length > 0) {
          setSelectedServiceId(response.data[0].id.toString());
        }
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách dịch vụ:', err);
      showToast('Không thể tải danh sách dịch vụ', 'error');
    }
  };

  const handleAddServiceSubmit = async (e) => {
    e.preventDefault();
    if (!selectedServiceId) {
      showToast('Vui lòng chọn dịch vụ', 'error');
      return;
    }
    
    setIsSubmittingService(true);
    try {
      await addServiceToBooking(
        selectedServiceBooking.id, 
        parseInt(selectedServiceId), 
        serviceQuantity, 
        serviceNote
      );
      showToast('Thêm dịch vụ thành công!', 'success');
      setIsAddServiceModalOpen(false);
      await fetchRealBookings();
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Không thể thêm dịch vụ.', 'error');
    } finally {
      setIsSubmittingService(false);
    }
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
      let backendSuccess = false;
      try {
        const response = await getAllBookings();
        if (response && response.data) {
          allBookings = response.data;
          backendSuccess = true;
        }
      } catch (err) {
        console.error('Lỗi khi tải toàn bộ đặt phòng từ backend:', err);
        try {
          const response = await getBookingHistory();
          if (response && response.data) {
            allBookings = [...response.data];
          }
        } catch (e2) { }
      }

      // Always merge local storage created bookings
      try {
        const localCreated = JSON.parse(localStorage.getItem('hotel_all_bookings') || '[]');
        localCreated.forEach(localBk => {
          if (!allBookings.some(b => b.bookingId === localBk.bookingId)) {
            allBookings.push(localBk);
          }
        });
      } catch (e3) { }

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
            status: (() => {
              const s = String(localStatus || '').trim();
              const sl = s.toLowerCase();
              if (sl === 'cancelled') return 'Cancelled';
              if (sl === 'checked in' || sl === 'checked-in' || sl === 'staying') return 'Checked In';
              if (sl === 'checked out' || sl === 'checked-out' || sl === 'completed') return 'Completed';
              if (sl === 'pending') return 'Pending';
              if (sl === 'paid') return 'Paid';
              if (sl === 'partially paid' || sl === 'partiallypaid') return 'Partially Paid';
              return s || 'Confirmed';
            })(),
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
            numberOfChildren: bk.numberOfChildren || 0,
            paidAmount: bk.paidAmount ?? 0,
            depositAmount: bk.depositAmount ?? 0,
            serviceChargeAmount: bk.serviceChargeAmount ?? 0,
            taxAmount: bk.taxAmount ?? 0,
            discountAmount: bk.discountAmount ?? 0,
            finalAmount: bk.finalAmount ?? bk.totalAmount ?? 0
          };
        });

        setBookings(realMapped);
      } else {
        setBookings([]);
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

  const handleViewDetail = (bk, fromTab) => {
    setSelectedBooking({ ...bk, prevTab: fromTab });
    fetchPendingRefunds();
    setActiveTab('booking-detail-view');
  };

  const triggerCustomConfirm = (title, message, onConfirm) => {
    setConfirmDialog({
      isOpen: true,
      title,
      message,
      onConfirm
    });
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
    const isCheckIn = scanningBooking.status === 'Confirmed' || scanningBooking.status === 'Paid' || scanningBooking.status === 'Partially Paid';
    const nextStatus = isCheckIn ? 'Checked In' : 'Completed';

    try {
      const response = isCheckIn
        ? await checkInBooking(scanningBooking.id)
        : await checkOutBooking(scanningBooking.id);
      const bookingResult = response?.data || {};

      localStorage.setItem(`booking_status_${scanningBooking.id}`, nextStatus);
      if (nextStatus === 'Checked In') {
        localStorage.setItem(`booking_actualcheckin_${scanningBooking.id}`, new Date().toISOString());
      } else if (nextStatus === 'Completed') {
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
      window.dispatchEvent(new Event('reload-bookings'));

      showToast(`Đã cập nhật trạng thái đơn ${scanningBooking.bookingReference} sang ${nextStatus === 'Checked In' ? 'ĐÃ NHẬN PHÒNG' : 'ĐÃ TRẢ PHÒNG (COMPLETED)'} thành công!`, 'success');
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
    const finalTargetStatus = isCheckIn ? 'Checked In' : 'Completed';
    try {
      const response = isCheckIn
        ? await checkInBooking(booking.id)
        : await checkOutBooking(booking.id);
      const bookingResult = response?.data || {};

      localStorage.setItem(`booking_status_${booking.id}`, finalTargetStatus);
      if (finalTargetStatus === 'Checked In') {
        localStorage.setItem(`booking_actualcheckin_${booking.id}`, new Date().toISOString());
      } else if (finalTargetStatus === 'Completed') {
        localStorage.setItem(`booking_actualcheckout_${booking.id}`, new Date().toISOString());
      }

      setBookings(prev => prev.map(bk =>
        bk.id === booking.id
          ? mergeBookingResult(bk, bookingResult, finalTargetStatus)
          : bk
      ));
      setSelectedBooking(current =>
        current?.id === booking.id
          ? mergeBookingResult(current, bookingResult, finalTargetStatus)
          : current
      );
      showToast(`Đã chuyển trạng thái sang ${finalTargetStatus === 'Checked In' ? 'ĐÃ NHẬN PHÒNG' : 'ĐÃ TRẢ PHÒNG (COMPLETED)'}!`, 'success');
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
    window.dispatchEvent(new Event('reload-bookings'));
    localStorage.setItem(`booking_status_${bookingId}`, 'Checked In');
    localStorage.setItem(`booking_actualcheckin_${bookingId}`, new Date().toISOString());
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
                setSelectedBooking={(bk) => handleViewDetail(bk, 'overview')}
                startScanner={startScanner}
                handleDirectCheckInOut={handleDirectCheckInOut}
                handleOpenWalkIn={() => setIsWalkInModalOpen(true)}
              />
            )}



            {/* QUẢN LÝ ĐẶT PHÒNG (BOOKINGS MANAGEMENT TABLE) */}
            {activeTab === 'bookings' && (
              <div className="space-y-6 animate-scale-in text-left">
                <div className="border-b border-neutral-900 pb-4">
                  <h3 className="text-white font-black text-base uppercase tracking-wider m-0">QUẢN LÝ ĐƠN ĐẶT PHÒNG</h3>
                  <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Quản lý và tra cứu toàn bộ danh sách đơn đặt phòng từ hệ thống</p>

                </div>
                <BookingsTable
                  showToast={showToast}
                  setSelectedBooking={(bk) => handleViewDetail(bk, 'bookings')}
                  startScanner={startScanner}
                  handleDirectCheckInOut={handleDirectCheckInOut}
                  handleViewInvoice={handleViewInvoice}
                  handleDownloadPdf={handleDownloadPdf}
                  isManager={isManager}
                  setActiveTab={setActiveTab}
                  onFaceCheckInSelect={(id) => {
                    setFaceCheckInBookingId(id);
                    setActiveTab('face-check-in');
                  }}
                  triggerCustomConfirm={triggerCustomConfirm}
                />
              </div>
            )}

            {/* QUẢN LÝ LOẠI PHÒNG (MANAGER ROOM TYPES CRUD) */}
            {activeTab === 'face-check-in' && (
              <FaceCheckInStation
                bookings={bookings}
                showToast={showToast}
                onCheckInCompleted={handleFaceCheckInCompleted}
                initialBookingId={faceCheckInBookingId}
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

            {/* CHI TIẾT ĐƠN ĐẶT PHÒNG NATIVE VIEW */}
            {activeTab === 'booking-detail-view' && selectedBooking && (() => {
              const balanceDue = (selectedBooking.finalAmount || selectedBooking.totalAmount || 0) - (selectedBooking.paidAmount || 0);
              const isConfirmed = selectedBooking.status === 'Confirmed' || selectedBooking.status === 'Paid' || selectedBooking.status === 'Partially Paid';
              const isCheckedIn = selectedBooking.status === 'Checked In' || selectedBooking.status === 'Checked-in' || selectedBooking.status === 'Staying';
              const usesFaceId = selectedBooking.checkInMethod === 'Face Recognition' || selectedBooking.checkInMethod === 'FaceID';
              const associatedRefund = pendingRefunds.find(req => req.bookingReference === selectedBooking.bookingReference);
              return (
                <div className="animate-scale-in text-left space-y-6">
                  <div className="flex justify-between items-center border-b border-neutral-900 pb-4">
                    <div>
                      <button
                        onClick={() => {
                          setActiveTab(selectedBooking.prevTab || 'bookings');
                        }}
                        className="text-primary hover:underline font-black text-[10px] uppercase tracking-widest border-none bg-transparent cursor-pointer flex items-center gap-1 mb-2"
                      >
                        <span className="material-symbols-outlined text-xs">arrow_back</span> Quay lại danh sách
                      </button>
                      <h3 className="text-white font-black text-lg uppercase tracking-wider m-0">CHI TIẾT ĐƠN ĐẶT PHÒNG</h3>
                      <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">
                        Mã đặt phòng: {selectedBooking.bookingReference}
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    {/* Left columns - Detailed info */}
                    <div className="lg:col-span-2 space-y-6">
                      
                      {/* Refund Request Block */}
                      {associatedRefund && (
                        <div className="bg-[#1c1114] border border-rose-900/50 p-6 rounded-sm flex flex-col gap-4 animate-scale-in">
                          <div className="flex items-center gap-2 border-b border-rose-950 pb-3">
                            <span className="material-symbols-outlined text-rose-500 text-base">payments</span>
                            <h4 className="text-xs font-black text-rose-400 uppercase tracking-widest m-0">Yêu cầu hoàn tiền chờ xử lý</h4>
                            <span className="ml-auto px-2 py-0.5 text-[8px] font-black text-rose-500 bg-rose-500/10 border border-rose-500/20 uppercase tracking-widest rounded-sm">Pending</span>
                          </div>
                          
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-xs">
                            <div className="space-y-2">
                              <div>
                                <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Lý do hoàn tiền từ khách:</span>
                                <span className="text-slate-300 font-medium italic">"{associatedRefund.description}"</span>
                              </div>
                              <div className="pt-1">
                                <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Gửi lúc:</span>
                                <span className="text-slate-400 font-semibold">{new Date(associatedRefund.createdAt).toLocaleString('vi-VN')}</span>
                              </div>
                            </div>

                            <div className="bg-neutral-950/40 p-4 border border-neutral-900/60 space-y-2 rounded-sm font-semibold">
                              <div className="flex justify-between text-[11px]">
                                <span className="text-slate-400">Số tiền ban đầu:</span>
                                <span className="text-white font-mono">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(parseFloat(associatedRefund.oldValue))}</span>
                              </div>
                              <div className="flex justify-between text-[11px] border-t border-neutral-900/40 pt-1.5">
                                <span className="text-rose-400">Hoàn tiền dự kiến:</span>
                                <strong className="text-rose-500 font-mono">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(parseFloat(associatedRefund.newValue))}</strong>
                              </div>
                            </div>
                          </div>

                          <div className="flex gap-3 justify-end border-t border-rose-950/40 pt-3">
                            <button
                              onClick={() => {
                                setSelectedRefund(associatedRefund);
                                setRefundOverrideAmount(associatedRefund.newValue);
                                setIsApproveRefundOpen(true);
                              }}
                              className="bg-green-600 hover:bg-green-700 text-white text-[9.5px] font-black uppercase tracking-widest px-4 py-2 cursor-pointer border-none rounded-sm transition-all"
                            >
                              Phê duyệt hoàn tiền
                            </button>
                            <button
                              onClick={() => {
                                setSelectedRefund(associatedRefund);
                                setRefundRejectionReason('');
                                setIsRejectRefundOpen(true);
                              }}
                              className="bg-rose-600 hover:bg-rose-700 text-white text-[9.5px] font-black uppercase tracking-widest px-4 py-2 cursor-pointer border-none rounded-sm transition-all"
                            >
                              Từ chối yêu cầu
                            </button>
                          </div>
                        </div>
                      )}

                      {/* Unified Info Card */}
                      <div className="bg-[#0b0b0d] border border-neutral-900/60 p-6 md:p-8 rounded-sm space-y-6">
                        <div className="flex items-center gap-2 border-b border-neutral-900 pb-3">
                          <span className="material-symbols-outlined text-primary text-base">info</span>
                          <h4 className="text-xs font-black text-white uppercase tracking-widest m-0">Thông tin chi tiết lưu trú</h4>
                        </div>
                        
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-6 text-xs">
                          {/* Col 1 */}
                          <div className="space-y-4">
                            <div className="flex flex-col gap-1.5 bg-[#0e0e11] p-4 border border-neutral-900/50 rounded-sm shadow-sm">
                              <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider">Khách hàng</span>
                              <strong className="text-white text-sm uppercase">{selectedBooking.guestName}</strong>
                              <span className="text-slate-400 text-[10px] mt-0.5">{selectedBooking.email || selectedBooking.guestEmail || 'N/A'} • {selectedBooking.guestPhone || 'N/A'}</span>
                            </div>

                            <div className="flex flex-col gap-1.5 bg-[#0e0e11] p-4 border border-neutral-900/50 rounded-sm shadow-sm">
                              <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider">Thời gian lưu trú</span>
                              <strong className="text-white text-[11px]">{selectedBooking.checkInDate} đến {selectedBooking.checkOutDate}</strong>
                              <span className="text-primary text-[10px] font-black uppercase tracking-wider mt-0.5">{selectedBooking.nights} đêm lưu trú</span>
                            </div>
                          </div>

                          {/* Col 2 */}
                          <div className="space-y-4">
                            <div className="flex flex-col gap-1.5 bg-[#0e0e11] p-4 border border-neutral-900/50 rounded-sm shadow-sm">
                              <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider">Phòng & Hạng phòng</span>
                              <strong className="text-white text-sm uppercase">{selectedBooking.roomType || 'N/A'}</strong>
                              <div className="mt-1">
                                {selectedBooking.roomNumber ? (
                                  <span className="inline-flex items-center gap-1 px-2.5 py-0.5 text-[9px] font-black text-primary bg-primary/10 border border-primary/20 rounded-sm">
                                    PHÒNG ASSIGNED: {selectedBooking.roomNumber}
                                  </span>
                                ) : (
                                  <span className="inline-flex items-center gap-1 px-2.5 py-0.5 text-[9px] font-black text-rose-500 bg-rose-500/10 border border-rose-500/20 rounded-sm uppercase tracking-wider">
                                    Chưa gán phòng
                                  </span>
                                )}
                              </div>
                            </div>

                            <div className="flex flex-col gap-1.5 bg-[#0e0e11] p-4 border border-neutral-900/50 rounded-sm shadow-sm">
                              <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider">Trạng thái & Check-in</span>
                              <div>
                                <span className={`inline-block px-2.5 py-0.5 text-[9px] font-black uppercase tracking-widest mr-2 rounded-sm ${
                                  isCheckedIn 
                                    ? 'bg-blue-900/20 text-blue-400 border border-blue-900/30' 
                                    : isConfirmed 
                                    ? 'bg-green-900/20 text-green-400 border border-green-900/30' 
                                    : 'bg-neutral-800 text-slate-500 border border-neutral-700/50'
                                }`}>
                                  {selectedBooking.status}
                                </span>
                                {selectedBooking.checkInMethod === 'FaceID' || selectedBooking.checkInMethod === 'Face Recognition' ? (
                                  <span className="inline-flex items-center gap-1 px-2 py-0.5 text-[9px] font-black text-primary bg-primary/10 border border-primary/20 uppercase tracking-widest rounded-sm">
                                    FaceID eKYC
                                  </span>
                                ) : (
                                  <span className="inline-flex items-center gap-1 px-2 py-0.5 text-[9px] font-black text-slate-400 bg-neutral-900 border border-neutral-800 uppercase tracking-widest rounded-sm">
                                    {selectedBooking.checkInMethod || 'Manual'}
                                  </span>
                                )}
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>

                      {/* Room Key cards & Special Requests combined */}
                      {(selectedBooking.roomAccesses?.length > 0 || selectedBooking.specialRequests) && (
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                          {/* Digital Keys Column */}
                          {selectedBooking.roomAccesses?.length > 0 && (
                            <div className="bg-[#0b0b0d] border border-neutral-900/60 p-6 rounded-sm flex flex-col gap-4">
                              <div className="flex items-center gap-2 border-b border-neutral-900 pb-2">
                                <span className="material-symbols-outlined text-primary text-base">vpn_key</span>
                                <h4 className="text-xs font-black text-white uppercase tracking-widest m-0">Khóa phòng số</h4>
                              </div>
                              <div className="space-y-3">
                                {selectedBooking.roomAccesses.map((access) => (
                                  <div key={access.roomId || access.roomNumber} className="border border-neutral-900/50 bg-[#0e0e11] p-4 flex justify-between items-center rounded-sm">
                                    <div>
                                      <span className="block text-xs text-white font-black">Phòng {access.roomNumber}</span>
                                      <span className="block text-[8px] text-slate-500 font-bold uppercase mt-0.5">Tầng {access.floorNumber ?? 'N/A'}</span>
                                    </div>
                                    <div className="text-right">
                                      {access.roomPassword ? (
                                        <strong className="block font-mono text-xs text-primary tracking-[0.12em] bg-neutral-900/60 border border-primary/20 px-2 py-0.5 rounded-sm">
                                          {access.roomPassword}
                                        </strong>
                                      ) : (
                                        <span className="inline-flex items-center gap-1 px-1.5 py-0.5 text-[8px] font-black text-rose-500 bg-rose-500/10 border border-rose-500/20 uppercase tracking-wider rounded-sm">
                                          Đã khóa
                                        </span>
                                      )}
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Special Requests Column */}
                          {selectedBooking.specialRequests && (
                            <div className="bg-[#0b0b0d] border border-neutral-900/60 p-6 rounded-sm flex flex-col gap-4">
                              <div className="flex items-center gap-2 border-b border-neutral-900 pb-2">
                                <span className="material-symbols-outlined text-primary text-base">rate_review</span>
                                <h4 className="text-xs font-black text-white uppercase tracking-widest m-0">Yêu cầu đặc biệt</h4>
                              </div>
                              <p className="text-xs text-slate-300 font-semibold bg-[#0e0e11] p-4 border border-neutral-900/50 rounded-sm m-0 italic flex-1 flex items-center justify-center text-center">
                                "{selectedBooking.specialRequests}"
                              </p>
                            </div>
                          )}
                        </div>
                      )}
                    </div>



                    {/* Right column - Invoice summary */}
                    <div className="bg-[#0f0f12] border border-neutral-900 p-6 md:p-8 flex flex-col justify-between h-fit space-y-6">
                      <div>
                        <h4 className="text-xs font-black text-white uppercase tracking-widest border-b border-neutral-900 pb-3 mb-4">Chi tiết hóa đơn</h4>
                        <div className="space-y-3 text-xs text-slate-400 font-semibold">
                          <div className="flex justify-between">
                            <span>Tổng tiền phòng:</span>
                            <span className="text-white font-mono">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedBooking.totalAmount || 0)}</span>
                          </div>
                          <div className="flex justify-between">
                            <span>Phí dịch vụ phụ thu:</span>
                            <span className="text-white font-mono">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedBooking.serviceChargeAmount || 0)}</span>
                          </div>
                          <div className="flex justify-between">
                            <span>Thuế VAT (10%):</span>
                            <span className="text-white font-mono">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedBooking.taxAmount || 0)}</span>
                          </div>
                          <div className="flex justify-between border-t border-neutral-900 pt-3 text-sm font-bold">
                            <span className="text-white">TỔNG CỘNG:</span>
                            <span className="text-primary font-mono">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedBooking.finalAmount || selectedBooking.totalAmount || 0)}</span>
                          </div>
                          <div className="flex justify-between text-emerald-400 border-t border-neutral-900/50 pt-2">
                            <span>Đã thanh toán:</span>
                            <span className="font-mono">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedBooking.paidAmount || 0)}</span>
                          </div>
                          <div className="flex justify-between text-rose-500 font-bold">
                            <span>Còn lại cần thu:</span>
                            <span className="font-mono">
                              {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(
                                Math.max(0, (selectedBooking.finalAmount || selectedBooking.totalAmount || 0) - (selectedBooking.paidAmount || 0))
                              )}
                            </span>
                          </div>
                        </div>
                      </div>

                      <div className="space-y-3">
                        <span className="block text-[9px] text-slate-500 uppercase font-black">Thao tác nghiệp vụ:</span>
                        <div className="flex flex-col gap-2">
                          <button
                            onClick={() => handleViewInvoice(selectedBooking.id)}
                            className="w-full py-2.5 bg-neutral-900 border border-neutral-800 hover:bg-neutral-800 text-white text-[10px] font-black uppercase tracking-widest cursor-pointer flex items-center justify-center gap-1.5"
                          >
                            <span className="material-symbols-outlined text-sm">receipt_long</span> Xem hóa đơn chi tiết
                          </button>
                          <button
                            onClick={() => handleDownloadPdf(selectedBooking.id, selectedBooking.bookingReference)}
                            className="w-full py-2.5 bg-neutral-900 border border-neutral-800 hover:bg-neutral-800 text-white text-[10px] font-black uppercase tracking-widest cursor-pointer flex items-center justify-center gap-1.5"
                          >
                            <span className="material-symbols-outlined text-sm">download</span> Xuất file PDF bảng kê
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })()}
          </main>
        </div>
      </div>


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
                  <div className="flex justify-between text-green-400 font-bold items-center">
                    <div className="flex items-center gap-2">
                      <span>Đã thanh toán:</span>
                      {(() => {
                        const paid = parseFloat(invoiceData.paidAmount || 0);
                        const total = parseFloat(invoiceData.finalAmount || 1);
                        const ratio = total > 0 ? paid / total : 0;
                        const isFullyPaid = ratio >= 0.999;
                        const isDeposit = ratio > 0 && ratio < 0.999;
                        if (isFullyPaid) {
                          return (
                            <span style={{
                              display: 'inline-flex', alignItems: 'center', gap: '4px',
                              background: 'linear-gradient(135deg, #16a34a, #15803d)',
                              color: '#fff', fontSize: '9px', fontWeight: 900,
                              letterSpacing: '0.08em', padding: '2px 8px',
                              borderRadius: '3px', textTransform: 'uppercase',
                              boxShadow: '0 0 8px rgba(22,163,74,0.5)'
                            }}>
                              ✓ Thanh toán 100%
                            </span>
                          );
                        } else if (isDeposit) {
                          return (
                            <span style={{
                              display: 'inline-flex', alignItems: 'center', gap: '4px',
                              background: 'linear-gradient(135deg, #d97706, #b45309)',
                              color: '#fff', fontSize: '9px', fontWeight: 900,
                              letterSpacing: '0.08em', padding: '2px 8px',
                              borderRadius: '3px', textTransform: 'uppercase',
                              boxShadow: '0 0 8px rgba(217,119,6,0.5)'
                            }}>
                              ⚡ Đặt cọc {Math.round(ratio * 100)}%
                            </span>
                          );
                        }
                        return null;
                      })()}
                    </div>
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
                {/* Inline Add Service Form */}
                {(() => {
                  const associatedBooking = bookings.find(b => b.id === invoiceData?.bookingId);
                  const isNotCompleted = associatedBooking && associatedBooking.status !== 'Completed';
                  return isNotCompleted && (
                    <div className="border-t border-neutral-900 pt-4 space-y-3">
                      <span className="block text-[9px] font-black text-slate-500 uppercase tracking-widest">
                        Thêm dịch vụ phụ thu (Minibar, Spa, Concierge...)
                      </span>
                      <form onSubmit={handleInlineServiceSubmit} className="grid grid-cols-1 sm:grid-cols-4 gap-3 bg-neutral-950 p-4 border border-neutral-855">
                        <div className="sm:col-span-2">
                          <label className="block text-[9px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                            Chọn dịch vụ
                          </label>
                          <select
                            value={inlineServiceId}
                            onChange={(e) => setInlineServiceId(e.target.value)}
                            className="w-full bg-[#0f0f12] border border-neutral-800 text-white text-xs px-2.5 py-2.5 focus:border-primary outline-none"
                            required
                          >
                            <option value="" disabled>-- Chọn dịch vụ --</option>
                            {servicesList.map(svc => (
                              <option key={svc.id} value={svc.id}>
                                {svc.name} - {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(svc.price)}
                              </option>
                            ))}
                          </select>
                        </div>
                        <div>
                          <label className="block text-[9px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                            Số lượng
                          </label>
                          <input
                            type="number"
                            min="1"
                            value={inlineServiceQuantity}
                            onChange={(e) => setInlineServiceQuantity(parseInt(e.target.value) || 1)}
                            className="w-full bg-[#0f0f12] border border-neutral-800 text-white text-xs px-2.5 py-2 focus:border-primary outline-none"
                            required
                          />
                        </div>
                        <div className="flex items-end">
                          <button
                            type="submit"
                            disabled={isSubmittingInlineService}
                            className="w-full bg-slate-900 hover:bg-neutral-850 text-white border border-neutral-800 text-[10px] font-black uppercase tracking-widest py-2.5 px-3 cursor-pointer flex items-center justify-center gap-1"
                          >
                            {isSubmittingInlineService ? 'Đang thêm...' : 'Thêm dịch vụ'}
                          </button>
                        </div>
                        <div className="sm:col-span-4">
                          <label className="block text-[9px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                            Ghi chú dịch vụ (tùy chọn)
                          </label>
                          <input
                            type="text"
                            value={inlineServiceNote}
                            onChange={(e) => setInlineServiceNote(e.target.value)}
                            placeholder="Ví dụ: Sử dụng 2 lon Pepsi từ Minibar"
                            className="w-full bg-[#0f0f12] border border-neutral-800 text-white text-xs px-2.5 py-2 focus:border-primary outline-none"
                          />
                        </div>
                      </form>
                    </div>
                  );
                })()}

                {/* Counter Payment Form */}
                {parseFloat(invoiceData.dueAmount) > 0 && (
                  <div className="border-t border-neutral-900 pt-4 space-y-3">
                    <span className="block text-[9px] font-black text-slate-500 uppercase tracking-widest">
                      Ghi nhận thanh toán tại quầy
                    </span>
                    <form onSubmit={handleManualPaymentSubmit} className="grid grid-cols-1 sm:grid-cols-3 gap-3 bg-neutral-950 p-4 border border-neutral-855">
                      <div>
                        <label className="block text-[9px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                          Phương thức
                        </label>
                        <select
                          value={manualPaymentMethod}
                          onChange={(e) => setManualPaymentMethod(e.target.value)}
                          className="w-full bg-[#0f0f12] border border-neutral-800 text-white text-xs px-2.5 py-2 focus:border-primary outline-none"
                        >
                          <option value="Cash">Tiền mặt (Cash)</option>
                          <option value="Bank Transfer">Chuyển khoản (Bank Transfer)</option>
                          <option value="Credit Card">Thẻ tín dụng (Credit Card)</option>
                        </select>
                      </div>
                      <div>
                        <label className="block text-[9px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                          Số tiền thanh toán (VND)
                        </label>
                        <input
                          type="number"
                          value={manualPaymentAmount}
                          onChange={(e) => setManualPaymentAmount(e.target.value)}
                          placeholder="Nhập số tiền"
                          className="w-full bg-[#0f0f12] border border-neutral-800 text-white text-xs px-2.5 py-2 focus:border-primary outline-none"
                          required
                        />
                      </div>
                      <div className="flex items-end">
                        <button
                          type="submit"
                          disabled={isSubmittingManualPayment}
                          className="w-full bg-primary hover:brightness-110 disabled:bg-neutral-800 disabled:text-slate-500 disabled:cursor-not-allowed text-white text-[10px] font-black uppercase tracking-widest py-2 px-3 border-none cursor-pointer flex items-center justify-center gap-1"
                        >
                          {isSubmittingManualPayment ? 'Đang xử lý...' : 'Xác nhận thanh toán'}
                        </button>
                      </div>
                      <div className="col-span-1 sm:col-span-3">
                        <label className="block text-[9px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                          Ghi chú
                        </label>
                        <input
                          type="text"
                          value={manualPaymentNotes}
                          onChange={(e) => setManualPaymentNotes(e.target.value)}
                          placeholder="Ví dụ: Khách thanh toán phần còn lại bằng tiền mặt"
                          className="w-full bg-[#0f0f12] border border-neutral-800 text-white text-xs px-2.5 py-2 focus:border-primary outline-none"
                        />
                      </div>
                    </form>
                  </div>
                )}

                <div className="flex flex-wrap gap-2 justify-end pt-4 border-t border-neutral-900">
                  {(() => {
                    const associatedBooking = bookings.find(b => b.id === invoiceData?.bookingId);
                    const isStaying = associatedBooking && ['Checked In', 'Checked-in', 'Staying'].includes(associatedBooking.status);
                    const isNotCompleted = associatedBooking && associatedBooking.status !== 'Completed';
                    return (
                      <>
                        {isStaying && isNotCompleted && (
                          <button
                            type="button"
                            onClick={async () => {
                              const dueAmount = parseFloat(invoiceData.dueAmount || 0);
                              if (dueAmount > 0) {
                                showToast(`Đơn đặt phòng chưa được thanh toán đầy đủ. Quý khách cần thanh toán thêm ${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(dueAmount)} trước khi trả phòng.`, 'error');
                                return;
                              }
                              try {
                                await handleDirectCheckInOut(associatedBooking, 'Completed');
                                setIsInvoiceModalOpen(false);
                              } catch (err) {
                                console.error(err);
                              }
                            }}
                            className="bg-primary hover:brightness-110 text-white font-black px-6 py-3 uppercase text-[10px] tracking-widest cursor-pointer border-none flex items-center gap-1.5"
                          >
                            <span className="material-symbols-outlined text-xs">done_all</span> Hoàn tất Checkout
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => handleDownloadPdf(invoiceData.bookingId, invoiceData.bookingReference)}
                          className="bg-neutral-900 hover:bg-neutral-800 border border-neutral-800 text-white font-black px-6 py-3 uppercase text-[10px] tracking-widest cursor-pointer flex items-center gap-1.5"
                        >
                          <span className="material-symbols-outlined text-xs">download</span> Xuất PDF
                        </button>

                      </>
                    );
                  })()}
                  <button
                    onClick={() => setIsInvoiceModalOpen(false)}
                    className="bg-neutral-900 hover:bg-neutral-850 border border-neutral-800 text-white font-bold px-6 py-3 uppercase text-[10px] tracking-widest cursor-pointer"
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

      {/* ADD SERVICE MODAL */}
      {isAddServiceModalOpen && selectedServiceBooking && (
        <div className="fixed inset-0 bg-neutral-950/80 z-50 flex items-center justify-center p-4">
          <div className="bg-[#0f0f12] border border-neutral-900 max-w-md w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl animate-scale-in text-white text-left font-['Montserrat']">
            <div className="flex justify-between items-center border-b border-neutral-850 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase text-red-500 font-bold">THÊM DỊCH VỤ PHÁT SINH</span>
                <h4 className="text-xs font-black uppercase text-white m-0 mt-0.5">
                  Phòng: {selectedServiceBooking.roomNumber || 'Chưa gán'} • {selectedServiceBooking.guestName}
                </h4>
              </div>
              <button
                onClick={() => setIsAddServiceModalOpen(false)}
                className="text-slate-400 hover:text-white border-none bg-transparent cursor-pointer flex items-center"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <form onSubmit={handleAddServiceSubmit} className="space-y-4 text-xs">
              <div className="space-y-2">
                <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Chọn dịch vụ</label>
                <select
                  value={selectedServiceId}
                  onChange={(e) => setSelectedServiceId(e.target.value)}
                  className="w-full bg-[#0f0f12] border border-neutral-800 text-white text-xs px-3 py-2.5 focus:border-primary outline-none [&>option]:bg-[#0f0f12]"
                  required
                >
                  <option value="" disabled>-- Chọn dịch vụ --</option>
                  {servicesList.map(svc => (
                    <option key={svc.id} value={svc.id}>
                      {svc.name} - {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(svc.price)}
                    </option>
                  ))}
                </select>
              </div>

              <div className="space-y-2">
                <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Số lượng</label>
                <input
                  type="number"
                  min="1"
                  value={serviceQuantity}
                  onChange={(e) => setServiceQuantity(parseInt(e.target.value) || 1)}
                  className="w-full bg-[#0f0f12] border border-neutral-800 text-white text-xs px-3 py-2.5 focus:border-primary outline-none"
                  required
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Ghi chú</label>
                <input
                  type="text"
                  value={serviceNote}
                  onChange={(e) => setServiceNote(e.target.value)}
                  placeholder="Ví dụ: Khách gọi thêm từ minibar"
                  className="w-full bg-[#0f0f12] border border-neutral-800 text-white text-xs px-3 py-2.5 focus:border-primary outline-none"
                />
              </div>

              <div className="flex gap-4 pt-4 border-t border-neutral-900">
                <button
                  type="submit"
                  disabled={isSubmittingService}
                  className="bg-primary text-white font-bold px-6 py-3 uppercase text-xs tracking-widest hover:brightness-110 transition-all cursor-pointer border-none flex-1 flex items-center justify-center gap-1.5"
                >
                  {isSubmittingService ? 'Đang lưu...' : 'Thêm dịch vụ'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsAddServiceModalOpen(false)}
                  className="bg-neutral-900 hover:bg-neutral-850 border border-neutral-800 text-white font-bold px-6 py-3 uppercase text-xs tracking-widest cursor-pointer flex-1"
                >
                  Hủy bỏ
                </button>
              </div>
            </form>
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

      {/* CUSTOM CONFIRM DIALOG */}
      {confirmDialog.isOpen && (
        <div className="fixed inset-0 bg-neutral-950/80 z-[6000] flex items-center justify-center p-4 backdrop-blur-sm animate-fade-in text-white text-left font-['Montserrat']">
          <div className="bg-[#0f0f12] border border-neutral-900 max-w-sm w-full p-6 flex flex-col gap-4 shadow-2xl animate-scale-in">
            <div className="flex items-center gap-2 text-rose-500">
              <span className="material-symbols-outlined text-lg">warning</span>
              <span className="text-[10px] font-black uppercase tracking-widest">{confirmDialog.title || "XÁC NHẬN"}</span>
            </div>
            <p className="text-xs text-slate-300 font-semibold leading-relaxed m-0">
              {confirmDialog.message}
            </p>
            <div className="flex justify-end gap-2.5 pt-2 text-[10px] font-black uppercase tracking-widest">
              <button
                type="button"
                onClick={() => setConfirmDialog(prev => ({ ...prev, isOpen: false }))}
                className="px-4 py-2.5 border border-neutral-800 text-slate-300 bg-transparent hover:bg-neutral-800 transition-all cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={() => {
                  if (confirmDialog.onConfirm) confirmDialog.onConfirm();
                  setConfirmDialog(prev => ({ ...prev, isOpen: false }));
                }}
                className="px-4 py-2.5 bg-rose-600 text-white hover:bg-rose-700 transition-all cursor-pointer border-none"
              >
                Đồng ý hủy
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
