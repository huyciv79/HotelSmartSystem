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
  cancelBooking,
  getBookingDetail,
  updateBooking
} from '../services/bookingService';
import {
  getPendingRefundRequests,
  approveRefundRequest,
  rejectRefundRequest
} from '../services/refundService';
import {
  getPendingRoomChangeRequests,
  approveRoomChangeRequest,
  rejectRoomChangeRequest
} from '../services/roomChangeService';
import {
  getPendingStayExtensionRequests,
  approveStayExtensionRequest,
  rejectStayExtensionRequest
} from '../services/stayExtensionService';
import {
  getPendingEarlyCheckOutRequests,
  approveEarlyCheckOutRequest,
  rejectEarlyCheckOutRequest
} from '../services/earlyCheckOutService';
import { getUserProfile } from '../services/userService';
import { getDashboardStats } from '../services/statisticService';
import Profile from './Profile';
import { useToast, ToastContainer } from '../components/Toast';
import { useLanguage } from '../context/LanguageContext';

// Staff dashboard sub-components
import StaffSidebar from '../components/staff/StaffSidebar';
import StaffHeader from '../components/staff/StaffHeader';
import StaffOverview from '../components/staff/StaffOverview';
import RoomTypesManager from '../components/staff/RoomTypesManager';
import RoomsManager from '../components/staff/RoomsManager';
import ServicesManager from '../components/staff/ServicesManager';
import RevenueReports from '../components/staff/RevenueReports';
import FaceCheckInStation from '../components/staff/FaceCheckInStation';
import QrCheckInStation from '../components/staff/QrCheckInStation';
import BookingsTable from '../components/staff/BookingsTable';

// Mock Bookings Data for Receptionist/Manager Operation simulation
const INITIAL_MOCK_BOOKINGS = [
  { id: 101, bookingReference: 'BK20260616173108820', guestName: 'Nguyễn Văn Minh', email: 'minh.nguyen@example.com', roomType: 'Suite River View', quantity: 1, checkInDate: '2026-06-16', checkOutDate: '2026-07-01', nights: 15, totalAmount: 75000000, status: 'Confirmed', checkInMethod: 'Face Recognition' },
  { id: 102, bookingReference: 'BK20260611183213122', guestName: 'Trần Thị Thảo', email: 'thao.tran@example.com', roomType: 'Premium Triple Room City View', quantity: 1, checkInDate: '2026-06-11', checkOutDate: '2026-06-21', nights: 10, totalAmount: 16000000, status: 'Confirmed', checkInMethod: 'QR Code' },
  { id: 103, bookingReference: 'BK20260611173944273', guestName: 'Lê Hoàng Nam', email: 'nam.le@example.com', roomType: 'Standard No Window', quantity: 1, checkInDate: '2026-06-11', checkOutDate: '2026-06-21', nights: 10, totalAmount: 16000000, status: 'Checked In', checkInMethod: 'Manual' },
  { id: 104, bookingReference: 'BK20260615104499120', guestName: 'Phạm Minh Đức', email: 'duc.pham@example.com', roomType: 'Suite River View', quantity: 2, checkInDate: '2026-06-16', checkOutDate: '2026-06-20', nights: 4, totalAmount: 40000000, status: 'Confirmed', checkInMethod: 'Face Recognition', bookingType: 'Group' },
];

export default function StaffDashboard({ setActivePage }) {
  const { t } = useLanguage();
  const { toasts, showToast: rawShowToast, dismissToast } = useToast();
  const showToast = (message, type, duration) => {
    rawShowToast(t(message, message), type, duration);
  };

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
  const [stats, setStats] = useState(null);
  const [statsLoading, setStatsLoading] = useState(false);
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

  // Edit booking modal states
  const [isEditBookingModalOpen, setIsEditBookingModalOpen] = useState(false);
  const [isSubmittingEditBooking, setIsSubmittingEditBooking] = useState(false);
  const [editBookingFormData, setEditBookingFormData] = useState({
    roomTypeId: '',
    checkInDate: '',
    checkOutDate: '',
    quantity: 1,
    numberOfAdults: 1,
    numberOfChildren: 0,
    specialRequests: '',
    discountAmount: 0
  });
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
    customerIdCardNumber: '',
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

  // Room Change request management states
  const [pendingRoomChanges, setPendingRoomChanges] = useState([]);
  const [selectedRoomChange, setSelectedRoomChange] = useState(null);
  const [isRejectRoomChangeOpen, setIsRejectRoomChangeOpen] = useState(false);
  const [roomChangeRejectionReason, setRoomChangeRejectionReason] = useState('');
  const [isSubmittingRoomChangeAction, setIsSubmittingRoomChangeAction] = useState(false);
  const [availableRoomsForChange, setAvailableRoomsForChange] = useState([]);
  const [selectedPhysicalRoomId, setSelectedPhysicalRoomId] = useState('');
  const [isLoadingAvailableRooms, setIsLoadingAvailableRooms] = useState(false);

  // Stay Extension request management states
  const [pendingStayExtensions, setPendingStayExtensions] = useState([]);
  const [selectedStayExtension, setSelectedStayExtension] = useState(null);
  const [isRejectStayExtensionOpen, setIsRejectStayExtensionOpen] = useState(false);
  const [stayExtensionRejectionReason, setStayExtensionRejectionReason] = useState('');
  const [isSubmittingStayExtensionAction, setIsSubmittingStayExtensionAction] = useState(false);

  // Early Check-out request management states
  const [pendingEarlyCheckOuts, setPendingEarlyCheckOuts] = useState([]);
  const [selectedEarlyCheckOut, setSelectedEarlyCheckOut] = useState(null);
  const [isRejectEarlyCheckOutOpen, setIsRejectEarlyCheckOutOpen] = useState(false);
  const [earlyCheckOutRejectionReason, setEarlyCheckOutRejectionReason] = useState('');
  const [isSubmittingEarlyCheckOutAction, setIsSubmittingEarlyCheckOutAction] = useState(false);

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
        size: 1000,
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

  const fetchPendingRoomChanges = async () => {
    try {
      const response = await getPendingRoomChangeRequests();
      if (response && response.success) {
        setPendingRoomChanges(response.data || []);
      }
    } catch (err) {
      console.error('Lỗi khi tải yêu cầu đổi phòng:', err);
    }
  };

  const fetchPendingStayExtensions = async () => {
    try {
      const response = await getPendingStayExtensionRequests();
      if (response && response.success) {
        setPendingStayExtensions(response.data || []);
      }
    } catch (err) {
      console.error('Lỗi khi tải yêu cầu gia hạn:', err);
    }
  };

  const fetchPendingEarlyCheckOuts = async () => {
    try {
      const response = await getPendingEarlyCheckOutRequests();
      if (response && response.success) {
        setPendingEarlyCheckOuts(response.data || []);
      }
    } catch (err) {
      console.error('Lỗi khi tải yêu cầu check-out sớm:', err);
    }
  };

  useEffect(() => {
    const associated = pendingRoomChanges.find(r => selectedBooking && (r.bookingId === selectedBooking.bookingId || r.bookingReference === selectedBooking.bookingReference));
    if (associated) {
      fetchAvailableRoomsForStaff(associated.newValue);
    } else {
      setAvailableRoomsForChange([]);
      setSelectedPhysicalRoomId('');
    }
  }, [pendingRoomChanges, selectedBooking]);

  useEffect(() => {
    if (activeTab === 'refunds') {
      fetchPendingRefunds();
    } else if (activeTab === 'overview' || activeTab === 'operations') {
      fetchRealBookings();
    }
    if (activeTab === 'overview' || activeTab === 'reports') {
      fetchStats();
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
    if (!walkInFormData.roomTypeId || !walkInFormData.checkInDate || !walkInFormData.checkOutDate || !walkInFormData.customerFullname || !walkInFormData.customerEmail || !walkInFormData.customerPhonenumber || !walkInFormData.customerIdCardNumber) {
      showToast('Vui lòng điền đầy đủ các thông tin bắt buộc', 'warning');
      return;
    }
    if (!/^[0-9]{12}$/.test(walkInFormData.customerIdCardNumber)) {
      showToast('So CCCD phai gom dung 12 chu so', 'warning');
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

  const handleEditBookingSubmit = async (e) => {
    e.preventDefault();
    setIsSubmittingEditBooking(true);
    try {
      const response = await updateBooking(selectedBooking.id, {
        roomTypeId: editBookingFormData.roomTypeId ? parseInt(editBookingFormData.roomTypeId) : null,
        checkInDate: editBookingFormData.checkInDate,
        checkOutDate: editBookingFormData.checkOutDate,
        quantity: editBookingFormData.quantity ? parseInt(editBookingFormData.quantity) : null,
        numberOfAdults: editBookingFormData.numberOfAdults ? parseInt(editBookingFormData.numberOfAdults) : null,
        numberOfChildren: editBookingFormData.numberOfChildren !== undefined ? parseInt(editBookingFormData.numberOfChildren) : 0,
        specialRequests: editBookingFormData.specialRequests,
        discountAmount: editBookingFormData.discountAmount ? parseFloat(editBookingFormData.discountAmount) : 0,
      });
      if (response && response.success) {
        showToast('Cập nhật đơn đặt phòng thành công!', 'success');
        setIsEditBookingModalOpen(false);
        const updated = response.data;
        setSelectedBooking(prev => ({
          ...prev,
          ...updated,
          id: updated.bookingId,
          bookingReference: updated.bookingNumber || updated.bookingReference || prev.bookingReference,
          roomType: updated.roomTypeName || prev.roomType,
          roomTypeId: updated.roomTypeId || prev.roomTypeId,
          checkInDate: updated.checkInDate || prev.checkInDate,
          checkOutDate: updated.checkOutDate || prev.checkOutDate,
          nights: updated.nights || prev.nights,
          totalAmount: updated.totalAmount || prev.totalAmount,
          finalAmount: updated.finalAmount || prev.finalAmount,
          paidAmount: updated.paidAmount || prev.paidAmount,
          taxAmount: updated.taxAmount || prev.taxAmount,
          specialRequests: updated.specialRequests || prev.specialRequests,
          numberOfAdults: updated.numberOfAdults || prev.numberOfAdults,
          numberOfChildren: updated.numberOfChildren || prev.numberOfChildren,
          quantity: updated.quantity || prev.quantity,
          discountAmount: updated.discountAmount || prev.discountAmount,
        }));
        fetchRealBookings();
      }
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Không thể cập nhật đơn đặt phòng.', 'error');
    } finally {
      setIsSubmittingEditBooking(false);
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

  const handleQuickApproveRefund = (refund) => {
    const formattedAmount = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(parseFloat(refund.newValue));
    triggerCustomConfirm(
      'PHÊ DUYỆT HOÀN TIỀN NHANH',
      `Xác nhận phê duyệt hoàn tiền ${formattedAmount} cho đơn đặt phòng ${refund.bookingReference}? (Sử dụng số tiền tính toán tự động)`,
      async () => {
        setIsSubmittingRefundAction(true);
        try {
          const response = await approveRefundRequest(refund.requestId, null);
          if (response && response.success) {
            showToast('Phê duyệt hoàn tiền thành công!', 'success');
            fetchPendingRefunds();
            fetchRealBookings();
          }
        } catch (err) {
          console.error(err);
          showToast(err.response?.data?.message || 'Lỗi khi phê duyệt hoàn tiền.', 'error');
        } finally {
          setIsSubmittingRefundAction(false);
        }
      },
      'Duyệt ngay'
    );
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
            roomTypeId: bk.roomTypeId || '',
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
        setSelectedBooking(prev => {
          if (!prev) return null;
          const fresh = realMapped.find(b => b.id === prev.id);
          return fresh ? { ...prev, ...fresh } : prev;
        });
      } else {
        setBookings([]);
      }
      fetchStats();
    } catch (err) {
      console.error('Lỗi trong fetchRealBookings:', err);
    }
  };

  const fetchStats = async () => {
    setStatsLoading(true);
    try {
      const response = await getDashboardStats();
      if (response && response.success) {
        setStats(response.data);
      }
    } catch (err) {
      console.error('Lỗi khi tải thông tin thống kê:', err);
    } finally {
      setStatsLoading(false);
    }
  };

  const fetchRoomTypes = async (keyword = '') => {
    try {
      const response = await getRoomTypes('all', keyword);
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
    fetchPendingRoomChanges();
    fetchPendingStayExtensions();
    fetchPendingEarlyCheckOuts();
    setActiveTab('booking-detail-view');
  };

  const triggerCustomConfirm = (title, message, onConfirm, confirmText) => {
    setConfirmDialog({
      isOpen: true,
      title,
      message,
      onConfirm,
      confirmText
    });
  };

  // ROOM CHANGE HANDLERS
  const fetchAvailableRoomsForStaff = async (roomTypeId) => {
    setIsLoadingAvailableRooms(true);
    try {
      const response = await getRooms({ status: 'Available', roomTypeId: roomTypeId, size: 1000 });
      if (response && response.success && response.data) {
        const list = response.data.content || response.data || [];
        // Lọc các phòng có roomTypeId khớp (để đề phòng fallback)
        const filtered = list.filter(r => {
          const typeId = r.roomTypeId || r.roomtypeid?.id || r.roomtypeid || (r.roomType && r.roomType.roomTypeId);
          return String(typeId) === String(roomTypeId);
        });
        setAvailableRoomsForChange(filtered);
      }
    } catch (err) {
      console.error('Lỗi khi tải phòng trống:', err);
      showToast('Không thể tải danh sách phòng trống', 'error');
    } finally {
      setIsLoadingAvailableRooms(false);
    }
  };

  const handleApproveRoomChange = async (req) => {
    if (!selectedPhysicalRoomId) {
      showToast('Vui lòng chọn phòng vật lý cụ thể để gán cho khách', 'warning');
      return;
    }
    setIsSubmittingRoomChangeAction(true);
    try {
      const response = await approveRoomChangeRequest(req.requestId, selectedPhysicalRoomId);
      if (response && response.success) {
        showToast('Phê duyệt yêu cầu chuyển phòng thành công!', 'success');
        // Reload dữ liệu
        fetchPendingRoomChanges();
        fetchRealBookings();
      }
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Phê duyệt chuyển phòng thất bại.', 'error');
    } finally {
      setIsSubmittingRoomChangeAction(false);
    }
  };

  const handleRejectRoomChange = async () => {
    if (!roomChangeRejectionReason.trim()) {
      showToast('Vui lòng nhập lý do từ chối', 'warning');
      return;
    }
    setIsSubmittingRoomChangeAction(true);
    try {
      const response = await rejectRoomChangeRequest(selectedRoomChange.requestId, roomChangeRejectionReason.trim());
      if (response && response.success) {
        showToast('Đã từ chối yêu cầu chuyển phòng.', 'success');
        setIsRejectRoomChangeOpen(false);
        setRoomChangeRejectionReason('');
        fetchPendingRoomChanges();
        fetchRealBookings();
      }
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Từ chối chuyển phòng thất bại.', 'error');
    } finally {
      setIsSubmittingRoomChangeAction(false);
    }
  };

  // STAY EXTENSION HANDLERS
  const handleApproveStayExtension = async (req) => {
    setIsSubmittingStayExtensionAction(true);
    try {
      const response = await approveStayExtensionRequest(req.requestId);
      if (response && response.success) {
        showToast('Phê duyệt yêu cầu gia hạn lưu trú thành công!', 'success');
        fetchPendingStayExtensions();
        fetchRealBookings();
      }
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Phê duyệt gia hạn thất bại.', 'error');
    } finally {
      setIsSubmittingStayExtensionAction(false);
    }
  };

  const handleRejectStayExtension = async () => {
    if (!stayExtensionRejectionReason.trim()) {
      showToast('Vui lòng nhập lý do từ chối', 'warning');
      return;
    }
    setIsSubmittingStayExtensionAction(true);
    try {
      const response = await rejectStayExtensionRequest(selectedStayExtension.requestId, stayExtensionRejectionReason.trim());
      if (response && response.success) {
        showToast('Đã từ chối yêu cầu gia hạn.', 'success');
        setIsRejectStayExtensionOpen(false);
        setStayExtensionRejectionReason('');
        fetchPendingStayExtensions();
        fetchRealBookings();
      }
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Từ chối yêu cầu gia hạn thất bại.', 'error');
    } finally {
      setIsSubmittingStayExtensionAction(false);
    }
  };

  // EARLY CHECKOUT HANDLERS
  const handleApproveEarlyCheckOut = async (req) => {
    setIsSubmittingEarlyCheckOutAction(true);
    try {
      const response = await approveEarlyCheckOutRequest(req.requestId);
      if (response && response.success) {
        showToast('Phê duyệt yêu cầu check-out sớm thành công!', 'success');
        fetchPendingEarlyCheckOuts();
        fetchRealBookings();
      }
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Phê duyệt check-out sớm thất bại.', 'error');
    } finally {
      setIsSubmittingEarlyCheckOutAction(false);
    }
  };

  const handleRejectEarlyCheckOut = async () => {
    if (!earlyCheckOutRejectionReason.trim()) {
      showToast('Vui lòng nhập lý do từ chối', 'warning');
      return;
    }
    setIsSubmittingEarlyCheckOutAction(true);
    try {
      const response = await rejectEarlyCheckOutRequest(selectedEarlyCheckOut.requestId, earlyCheckOutRejectionReason.trim());
      if (response && response.success) {
        showToast('Đã từ chối yêu cầu check-out sớm.', 'success');
        setIsRejectEarlyCheckOutOpen(false);
        setEarlyCheckOutRejectionReason('');
        fetchPendingEarlyCheckOuts();
        fetchRealBookings();
      }
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Từ chối yêu cầu check-out sớm thất bại.', 'error');
    } finally {
      setIsSubmittingEarlyCheckOutAction(false);
    }
  };

  // Check-in & Check-out simulations
  const startScanner = (booking, type) => {
    if (type === 'qr') {
      setActiveTab('qr-check-in');
      return;
    }

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

      <div className="min-h-screen bg-slate-50 flex font-['Montserrat'] text-slate-800 text-left pt-0">

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
                stats={stats}
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

                {isScanning && scanningBooking && (
                  <div className="bg-white border border-slate-200/80 p-6 text-slate-800 text-center shadow-xl relative animate-scale-in">
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
                <div className="bg-white border border-slate-250/80 shadow-md overflow-hidden">
                  <div className="p-4 border-b border-slate-100 bg-slate-50/50 text-[10px] font-black uppercase text-slate-500 tracking-wider">
                    Danh sách đặt phòng cần xử lý trong ngày
                  </div>

                  <div className="divide-y divide-slate-100">
                    {filteredBookings.length === 0 ? (
                      <div className="py-12 text-center text-slate-500 font-bold text-xs uppercase tracking-widest">
                        Không tìm thấy lịch trình đặt phòng nào phù hợp
                      </div>
                    ) : (
                      filteredBookings.map((bk) => {
                        const normalizedStatus = String(bk.status || '').toLowerCase().replace('-', ' ');
                        const isConfirmed = ['confirmed', 'paid', 'partially paid'].includes(normalizedStatus);
                        const isCheckedIn = normalizedStatus === 'checked in';
                        const isCancelled = normalizedStatus === 'cancelled';
                        const normalizedMethod = String(bk.checkInMethod || '').toLowerCase();
                        const usesFaceId =
                          normalizedMethod === 'face recognition' ||
                          normalizedMethod === 'faceid' ||
                          normalizedMethod === 'face id';
                        const usesQrCode = normalizedMethod === 'qr code' || normalizedMethod === 'qr';

                        let statusBadgeClass = 'bg-slate-400 text-white border-none';
                        if (isCancelled) {
                          statusBadgeClass = 'bg-rose-500 text-white border-none';
                        } else if (isCheckedIn) {
                          statusBadgeClass = 'bg-blue-600 text-white border-none';
                        } else if (isConfirmed) {
                          statusBadgeClass = 'bg-green-600 text-white border-none';
                        }

                        return (
                          <div key={bk.id} className="p-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-6 hover:bg-slate-50/50 transition-colors">
                            <div className="space-y-1">
                              <span className="inline-block px-2 py-0.5 text-[8px] font-black tracking-widest text-slate-500 bg-slate-100 border border-slate-200 uppercase mb-1 rounded-sm">
                                {bk.bookingType && bk.bookingType.toLowerCase() === 'group' ? 'ĐOÀN (GROUP)' : 'ĐƠN LẺ'}
                              </span>
                              <h5 className="text-sm font-black text-slate-800 uppercase tracking-wider m-0">
                                {bk.guestName}
                              </h5>
                              <p className="text-[10px] text-slate-650 font-bold uppercase tracking-wider">
                                Mã: {bk.bookingReference} • {bk.roomType}
                              </p>
                              <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">
                                Ngày lưu trú: {bk.checkInDate} đến {bk.checkOutDate} ({bk.nights} đêm)
                              </p>
                            </div>
                            <div className="flex flex-col items-end gap-2">
                              <span className={`px-2.5 py-1 text-[8.5px] font-black uppercase tracking-widest rounded-full ${statusBadgeClass}`}>
                                {bk.status}
                              </span>

                              <div className="flex gap-2">
                                <button
                                  onClick={() => setSelectedBooking(bk)}
                                  className="bg-slate-100 border border-slate-200 text-slate-700 text-[9px] font-black uppercase tracking-widest px-3 py-2 cursor-pointer flex items-center gap-1 hover:bg-slate-200 hover:text-slate-900 rounded-sm transition-all"
                                >
                                  <span className="material-symbols-outlined text-xs">info</span> Chi tiết
                                </button>
                                <button
                                  onClick={() => handleViewInvoice(bk.id)}
                                  className="bg-slate-100 border border-slate-200 text-slate-700 text-[9px] font-black uppercase tracking-widest px-3 py-2 cursor-pointer flex items-center gap-1 hover:bg-slate-200 hover:text-slate-900 rounded-sm transition-all"
                                >
                                  <span className="material-symbols-outlined text-xs">receipt_long</span> Hóa đơn
                                </button>

                                <button
                                  onClick={() => handleDownloadPdf(bk.id, bk.bookingReference)}
                                  className="bg-slate-100 border border-slate-200 text-slate-700 text-[9px] font-black uppercase tracking-widest px-3 py-2 cursor-pointer flex items-center gap-1 hover:bg-slate-200 hover:text-slate-900 rounded-sm transition-all"
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
                                        if (usesQrCode) {
                                          startScanner(bk, 'qr');
                                          return;
                                        }
                                        handleDirectCheckInOut(bk, 'Checked In');
                                      }}
                                      disabled={usesFaceId && !isManager}
                                      className="bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-100 disabled:text-slate-400 disabled:cursor-not-allowed text-white text-[9px] font-black uppercase tracking-widest px-3 py-2 border-none cursor-pointer flex items-center gap-1.5 rounded-sm shadow-sm transition-all"
                                    >
                                      <span className="material-symbols-outlined text-xs">
                                        {usesFaceId ? 'face' : usesQrCode ? 'qr_code_scanner' : 'how_to_reg'}
                                      </span>
                                      {usesFaceId
                                        ? isManager
                                          ? 'FaceID tại sảnh'
                                          : 'Cần Manager'
                                        : usesQrCode
                                        ? 'Quét QR'
                                        : 'Check-in tại quầy'}
                                    </button>
                                  </>
                                )}
                                {isCheckedIn && (
                                  <button
                                    onClick={() => handleDirectCheckInOut(bk, 'Checked Out')}
                                    className="bg-indigo-600 hover:bg-indigo-750 text-white text-[9px] font-black uppercase tracking-widest px-4 py-2 border-none cursor-pointer rounded-sm shadow-sm transition-all"
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

            {/* QUẢN LÝ ĐẶT PHÒNG (BOOKINGS MANAGEMENT TABLE) */}
            {activeTab === 'bookings' && (
              <div className="space-y-6 animate-scale-in text-left">
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
                  handleOpenWalkIn={() => setIsWalkInModalOpen(true)}
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

            {activeTab === 'qr-check-in' && (
              <QrCheckInStation
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
                fetchRoomTypes={fetchRoomTypes}
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

            {/* QUẢN LÝ DANH SÁCH DỊCH VỤ (MANAGER SERVICES CRUD) */}
            {activeTab === 'services' && isManager && (
              <ServicesManager
                showToast={showToast}
                triggerCustomConfirm={triggerCustomConfirm}
              />
            )}

            {/* BÁO CÁO DOANH THU (MANAGER REPORTS) */}
            {activeTab === 'reports' && isManager && (
              <RevenueReports stats={stats} />
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
                  <div className="flex justify-between items-center pb-2">
                    <div>
                      <button
                        onClick={() => {
                          setActiveTab(selectedBooking.prevTab || 'bookings');
                        }}
                        className="text-primary hover:underline font-black text-[10px] uppercase tracking-widest border-none bg-transparent cursor-pointer flex items-center gap-1 mb-3"
                      >
                        <span className="material-symbols-outlined text-xs">arrow_back</span> Quay lại danh sách
                      </button>
                      <h3 className="text-slate-800 font-black text-base uppercase tracking-wider m-0">Chi tiết đơn đặt phòng</h3>
                      <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest mt-1">
                        Mã đặt phòng: {selectedBooking.bookingReference}
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    {/* Left columns - Detailed info */}
                    <div className="lg:col-span-2 space-y-6">
                      
                      {/* Refund Request Block */}
                      {associatedRefund && (
                        <div className="bg-rose-50/50 border border-rose-100 rounded-2xl p-6 flex flex-col gap-4 animate-scale-in">
                          <div className="flex items-center gap-2 border-b border-rose-100 pb-3">
                            <span className="material-symbols-outlined text-rose-500 text-base">payments</span>
                            <h4 className="text-xs font-black text-rose-700 uppercase tracking-widest m-0">Yêu cầu hoàn tiền chờ xử lý</h4>
                            <span className="ml-auto px-2.5 py-1 text-[8.5px] font-extrabold text-rose-600 bg-rose-100 uppercase tracking-widest rounded-lg">Pending</span>
                          </div>
                          
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-xs">
                            <div className="space-y-2">
                              <div>
                                <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Lý do hoàn tiền từ khách:</span>
                                <span className="text-slate-700 font-semibold italic">"{associatedRefund.description}"</span>
                              </div>
                              <div className="pt-1">
                                <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Gửi lúc:</span>
                                <span className="text-slate-500 font-bold">{new Date(associatedRefund.createdAt).toLocaleString('vi-VN')}</span>
                              </div>
                            </div>

                            <div className="bg-white p-4 border border-rose-100 space-y-2 rounded-xl font-semibold">
                              <div className="flex justify-between text-[11px]">
                                <span className="text-slate-500">Số tiền ban đầu:</span>
                                <span className="text-slate-800 font-mono">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(parseFloat(associatedRefund.oldValue))}</span>
                              </div>
                              <div className="flex justify-between text-[11px] border-t border-rose-100 pt-1.5">
                                <span className="text-rose-600">Hoàn tiền dự kiến:</span>
                                <strong className="text-rose-600 font-mono">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(parseFloat(associatedRefund.newValue))}</strong>
                              </div>
                            </div>
                          </div>

                          <div className="flex flex-wrap gap-2.5 justify-end border-t border-rose-100 pt-3">
                            <button
                              onClick={() => handleQuickApproveRefund(associatedRefund)}
                              disabled={isSubmittingRefundAction}
                              className="bg-emerald-600 hover:bg-emerald-700 text-white text-[9.5px] font-black uppercase tracking-widest px-4 py-2.5 cursor-pointer rounded-xl border-none shadow-sm transition-all flex items-center gap-1"
                            >
                              <span className="material-symbols-outlined text-xs">bolt</span>
                              Duyệt nhanh ({new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(parseFloat(associatedRefund.newValue))})
                            </button>
                            <button
                              onClick={() => {
                                setSelectedRefund(associatedRefund);
                                setRefundOverrideAmount(associatedRefund.newValue);
                                setIsApproveRefundOpen(true);
                              }}
                              className="bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 text-[9.5px] font-bold uppercase tracking-wider px-3.5 py-2.5 cursor-pointer rounded-xl transition-all flex items-center gap-1"
                            >
                              <span className="material-symbols-outlined text-xs">edit</span>
                              Điều chỉnh số tiền...
                            </button>
                            <button
                              onClick={() => {
                                setSelectedRefund(associatedRefund);
                                setRefundRejectionReason('');
                                setIsRejectRefundOpen(true);
                              }}
                              className="bg-rose-600 hover:bg-rose-700 text-white text-[9.5px] font-black uppercase tracking-widest px-4 py-2.5 cursor-pointer rounded-xl border-none shadow-sm transition-all flex items-center gap-1"
                            >
                              Từ chối
                            </button>
                          </div>
                        </div>
                      )}

                      {/* Room Move Request Block */}
                      {(() => {
                        const associatedRoomChange = pendingRoomChanges.find(req => req.bookingId === selectedBooking.bookingId || req.bookingReference === selectedBooking.bookingReference);
                        if (!associatedRoomChange) return null;
                        return (
                          <div className="bg-blue-50/50 border border-blue-100 rounded-2xl p-6 flex flex-col gap-4 animate-scale-in">
                            <div className="flex items-center gap-2 border-b border-blue-100 pb-3">
                              <span className="material-symbols-outlined text-blue-500 text-base">autorenew</span>
                              <h4 className="text-xs font-black text-blue-700 uppercase tracking-widest m-0">Yêu cầu đổi phòng chờ xử lý</h4>
                              <span className="ml-auto px-2.5 py-1 text-[8.5px] font-extrabold text-blue-600 bg-blue-100 uppercase tracking-widest rounded-lg">Pending</span>
                            </div>
                            
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-xs">
                              <div className="space-y-2">
                                <div>
                                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Lý do yêu cầu đổi phòng:</span>
                                  <span className="text-slate-700 font-semibold italic">"{associatedRoomChange.description || 'Không có lý do chi tiết'}"</span>
                                </div>
                                <div className="pt-1">
                                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Yêu cầu lúc:</span>
                                  <span className="text-slate-500 font-bold">{new Date(associatedRoomChange.createdAt).toLocaleString('vi-VN')}</span>
                                </div>
                              </div>

                              <div className="bg-white p-4 border border-blue-100 space-y-3 rounded-xl font-semibold">
                                <div className="flex justify-between text-[11px]">
                                  <span className="text-slate-500">Hạng phòng đích:</span>
                                  <span className="text-slate-800 font-bold">{associatedRoomChange.newValue === String(selectedBooking.roomTypeId) ? 'Cùng hạng phòng hiện tại' : 'Hạng phòng khác'}</span>
                                </div>
                                
                                <div className="space-y-1">
                                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Chọn phòng vật lý trống để gán:</span>
                                  {isLoadingAvailableRooms ? (
                                    <div className="text-[10px] text-slate-500">Đang tải danh sách phòng...</div>
                                  ) : availableRoomsForChange.length === 0 ? (
                                    <div className="text-[10px] text-rose-500">Hết phòng trống thuộc hạng phòng này!</div>
                                  ) : (
                                    <select
                                      value={selectedPhysicalRoomId}
                                      onChange={(e) => setSelectedPhysicalRoomId(e.target.value)}
                                      className="w-full bg-white border border-slate-200 text-slate-800 p-2 text-xs font-semibold focus:outline-none"
                                    >
                                      <option value="">-- Chọn phòng vật lý --</option>
                                      {availableRoomsForChange.map(r => (
                                        <option key={r.id} value={r.id}>Phòng {r.roomNumber || r.roomnumber} (Tầng {r.floorNumber ?? r.floornumber})</option>
                                      ))}
                                    </select>
                                  )}
                                </div>
                              </div>
                            </div>

                            <div className="flex gap-3 justify-end border-t border-blue-100 pt-3">
                              <button
                                onClick={() => handleApproveRoomChange(associatedRoomChange)}
                                disabled={isSubmittingRoomChangeAction}
                                className="bg-emerald-600 hover:bg-emerald-700 text-white text-[9.5px] font-black uppercase tracking-widest px-4 py-2.5 cursor-pointer border-none rounded-xl shadow-sm transition-all"
                              >
                                {isSubmittingRoomChangeAction ? 'Đang duyệt...' : 'Duyệt chuyển phòng'}
                              </button>
                              <button
                                onClick={() => {
                                  setSelectedRoomChange(associatedRoomChange);
                                  setRoomChangeRejectionReason('');
                                  setIsRejectRoomChangeOpen(true);
                                }}
                                disabled={isSubmittingRoomChangeAction}
                                className="bg-rose-600 hover:bg-rose-700 text-white text-[9.5px] font-black uppercase tracking-widest px-4 py-2.5 cursor-pointer border-none rounded-xl shadow-sm transition-all"
                              >
                                Từ chối
                              </button>
                            </div>
                          </div>
                        );
                      })()}

                      {/* Stay Extension Request Block */}
                      {(() => {
                        const associatedStayExtension = pendingStayExtensions.find(req => req.bookingId === selectedBooking.bookingId || req.bookingReference === selectedBooking.bookingReference);
                        if (!associatedStayExtension) return null;
                        return (
                          <div className="bg-emerald-50/50 border border-emerald-100 rounded-2xl p-6 flex flex-col gap-4 animate-scale-in">
                            <div className="flex items-center gap-2 border-b border-emerald-100 pb-3">
                              <span className="material-symbols-outlined text-emerald-500 text-base">calendar_add_on</span>
                              <h4 className="text-xs font-black text-emerald-700 uppercase tracking-widest m-0">Yêu cầu gia hạn lưu trú chờ xử lý</h4>
                              <span className="ml-auto px-2.5 py-1 text-[8.5px] font-extrabold text-emerald-600 bg-emerald-100 uppercase tracking-widest rounded-lg">Pending</span>
                            </div>
                            
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-xs">
                              <div className="space-y-2">
                                <div>
                                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Lý do yêu cầu từ khách:</span>
                                  <span className="text-slate-700 font-semibold italic">"{associatedStayExtension.description || 'Không có lý do chi tiết'}"</span>
                                </div>
                                <div className="pt-1">
                                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Gửi lúc:</span>
                                  <span className="text-slate-500 font-bold">{new Date(associatedStayExtension.createdAt).toLocaleString('vi-VN')}</span>
                                </div>
                              </div>

                              <div className="bg-white p-4 border border-emerald-100 space-y-2 rounded-xl font-semibold">
                                <div className="flex justify-between text-[11px]">
                                  <span className="text-slate-500">Ngày check-out hiện tại:</span>
                                  <span className="text-slate-800 font-mono">{associatedStayExtension.oldValue}</span>
                                </div>
                                <div className="flex justify-between text-[11px] border-t border-emerald-100/50 pt-1.5">
                                  <span className="text-emerald-600">Ngày check-out mong muốn:</span>
                                  <strong className="text-emerald-600 font-mono">{associatedStayExtension.newValue}</strong>
                                </div>
                              </div>
                            </div>

                            <div className="flex gap-3 justify-end border-t border-emerald-100 pt-3">
                              <button
                                onClick={() => handleApproveStayExtension(associatedStayExtension)}
                                disabled={isSubmittingStayExtensionAction}
                                className="bg-emerald-600 hover:bg-emerald-700 text-white text-[9.5px] font-black uppercase tracking-widest px-4 py-2.5 cursor-pointer border-none rounded-xl shadow-sm transition-all"
                              >
                                {isSubmittingStayExtensionAction ? 'Đang duyệt...' : 'Duyệt gia hạn'}
                              </button>
                              <button
                                onClick={() => {
                                  setSelectedStayExtension(associatedStayExtension);
                                  setStayExtensionRejectionReason('');
                                  setIsRejectStayExtensionOpen(true);
                                }}
                                disabled={isSubmittingStayExtensionAction}
                                className="bg-rose-600 hover:bg-rose-700 text-white text-[9.5px] font-black uppercase tracking-widest px-4 py-2.5 cursor-pointer border-none rounded-xl shadow-sm transition-all"
                              >
                                Từ chối
                              </button>
                            </div>
                          </div>
                        );
                      })()}

                      {/* Early Check-out Request Block */}
                      {(() => {
                        const associatedEarlyCheckOut = pendingEarlyCheckOuts.find(req => req.bookingId === selectedBooking.bookingId || req.bookingReference === selectedBooking.bookingReference);
                        if (!associatedEarlyCheckOut) return null;
                        return (
                          <div className="bg-amber-50/50 border border-amber-100 rounded-2xl p-6 flex flex-col gap-4 animate-scale-in">
                            <div className="flex items-center gap-2 border-b border-amber-100 pb-3">
                              <span className="material-symbols-outlined text-amber-500 text-base">history</span>
                              <h4 className="text-xs font-black text-amber-700 uppercase tracking-widest m-0">Yêu cầu Checkout sớm chờ xử lý</h4>
                              <span className="ml-auto px-2.5 py-1 text-[8.5px] font-extrabold text-amber-600 bg-amber-100 uppercase tracking-widest rounded-lg">Pending</span>
                            </div>
                            
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-xs">
                              <div className="space-y-2">
                                <div>
                                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Lý do yêu cầu từ khách:</span>
                                  <span className="text-slate-700 font-semibold italic">"{associatedEarlyCheckOut.description || 'Không có lý do chi tiết'}"</span>
                                </div>
                                <div className="pt-1">
                                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Gửi lúc:</span>
                                  <span className="text-slate-500 font-bold">{new Date(associatedEarlyCheckOut.createdAt).toLocaleString('vi-VN')}</span>
                                </div>
                              </div>

                              <div className="bg-white p-4 border border-amber-100 space-y-2 rounded-xl font-semibold">
                                <div className="flex justify-between text-[11px]">
                                  <span className="text-slate-500">Ngày check-out dự kiến gốc:</span>
                                  <span className="text-slate-800 font-mono">{associatedEarlyCheckOut.oldValue}</span>
                                </div>
                                <div className="flex justify-between text-[11px] border-t border-amber-100/50 pt-1.5">
                                  <span className="text-amber-600">Ngày check-out mới rút ngắn:</span>
                                  <strong className="text-amber-600 font-mono">{associatedEarlyCheckOut.newValue}</strong>
                                </div>
                              </div>
                            </div>

                            <div className="flex gap-3 justify-end border-t border-amber-100 pt-3">
                              <button
                                onClick={() => handleApproveEarlyCheckOut(associatedEarlyCheckOut)}
                                disabled={isSubmittingEarlyCheckOutAction}
                                className="bg-emerald-600 hover:bg-emerald-700 text-white text-[9.5px] font-black uppercase tracking-widest px-4 py-2.5 cursor-pointer border-none rounded-xl shadow-sm transition-all"
                              >
                                {isSubmittingEarlyCheckOutAction ? 'Đang duyệt...' : 'Duyệt rút ngắn ngày'}
                              </button>
                              <button
                                onClick={() => {
                                  setSelectedEarlyCheckOut(associatedEarlyCheckOut);
                                  setEarlyCheckOutRejectionReason('');
                                  setIsRejectEarlyCheckOutOpen(true);
                                }}
                                disabled={isSubmittingEarlyCheckOutAction}
                                className="bg-rose-600 hover:bg-rose-700 text-white text-[9.5px] font-black uppercase tracking-widest px-4 py-2.5 cursor-pointer border-none rounded-xl shadow-sm transition-all"
                              >
                                Từ chối
                              </button>
                            </div>
                          </div>
                        );
                      })()}

                      {/* Unified Info Card */}
                      <div className="bg-white border border-slate-100 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.015)] p-6 md:p-8 space-y-6">
                        <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
                          <span className="material-symbols-outlined text-primary text-base">info</span>
                          <h4 className="text-xs font-black text-slate-800 uppercase tracking-widest m-0">Thông tin chi tiết lưu trú</h4>
                        </div>
                        
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-6 text-xs">
                          {/* Col 1 */}
                          <div className="space-y-4">
                            <div className="flex flex-col gap-1.5 bg-slate-50 p-4 border border-slate-100 rounded-xl">
                              <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">Khách hàng</span>
                              <strong className="text-slate-800 text-sm uppercase">{selectedBooking.guestName}</strong>
                              <span className="text-slate-500 text-[10px] mt-0.5">{selectedBooking.email || selectedBooking.guestEmail || 'N/A'} • {selectedBooking.guestPhone || 'N/A'}</span>
                            </div>

                            <div className="flex flex-col gap-1.5 bg-slate-50 p-4 border border-slate-100 rounded-xl">
                              <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">Thời gian lưu trú</span>
                              <strong className="text-slate-800 text-[11px]">{selectedBooking.checkInDate} → {selectedBooking.checkOutDate}</strong>
                              <span className="text-primary text-[10px] font-black uppercase tracking-wider mt-0.5">{selectedBooking.nights} đêm lưu trú</span>
                            </div>
                          </div>

                          {/* Col 2 */}
                          <div className="space-y-4">
                            <div className="flex flex-col gap-1.5 bg-slate-50 p-4 border border-slate-100 rounded-xl">
                              <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">Phòng & Hạng phòng</span>
                              <strong className="text-slate-800 text-sm uppercase">{selectedBooking.roomType || 'N/A'}</strong>
                              <div className="mt-1">
                                {selectedBooking.roomNumber ? (
                                  <span className="inline-flex items-center gap-1 px-2.5 py-1 text-[8.5px] font-extrabold text-primary bg-primary/10 border border-primary/20 rounded-lg">
                                    PHÒNG ASSIGNED: {selectedBooking.roomNumber}
                                  </span>
                                ) : (
                                  <span className="inline-flex items-center gap-1 px-2.5 py-1 text-[8.5px] font-extrabold text-rose-500 bg-rose-50 border border-rose-100 rounded-lg uppercase tracking-wider">
                                    Chưa gán phòng
                                  </span>
                                )}
                              </div>
                            </div>

                            <div className="flex flex-col gap-1.5 bg-slate-50 p-4 border border-slate-100 rounded-xl">
                              <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">Trạng thái & Check-in</span>
                              <div className="flex flex-wrap gap-1.5 mt-0.5">
                                <span className={`inline-block px-2.5 py-1 text-[8.5px] font-extrabold uppercase tracking-widest rounded-lg ${
                                  isCheckedIn 
                                    ? 'bg-blue-50 text-blue-600 border border-blue-100' 
                                    : isConfirmed 
                                    ? 'bg-emerald-50 text-emerald-600 border border-emerald-100' 
                                    : 'bg-slate-100 text-slate-600 border border-slate-200'
                                }`}>
                                  {selectedBooking.status}
                                </span>
                                {selectedBooking.checkInMethod === 'FaceID' || selectedBooking.checkInMethod === 'Face Recognition' ? (
                                  <span className="inline-flex items-center gap-1 px-2.5 py-1 text-[8.5px] font-extrabold text-primary bg-primary/10 border border-primary/20 uppercase tracking-widest rounded-lg">
                                    FaceID eKYC
                                  </span>
                                ) : (
                                  <span className="inline-flex items-center gap-1 px-2.5 py-1 text-[8.5px] font-extrabold text-slate-500 bg-slate-100 border border-slate-200 uppercase tracking-widest rounded-lg">
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
                            <div className="bg-white border border-slate-100 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.015)] p-6 flex flex-col gap-4">
                              <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
                                <span className="material-symbols-outlined text-primary text-base">vpn_key</span>
                                <h4 className="text-xs font-black text-slate-800 uppercase tracking-widest m-0">Khóa phòng số</h4>
                              </div>
                              <div className="space-y-3">
                                {selectedBooking.roomAccesses.map((access) => (
                                  <div key={access.roomId || access.roomNumber} className="border border-slate-100 bg-slate-50 p-4 flex justify-between items-center rounded-xl font-semibold">
                                    <div>
                                      <span className="block text-xs text-slate-800 font-black">Phòng {access.roomNumber}</span>
                                      <span className="block text-[8px] text-slate-400 font-bold uppercase mt-0.5">Tầng {access.floorNumber ?? 'N/A'}</span>
                                    </div>
                                    <div className="text-right">
                                      {access.roomPassword ? (
                                        <strong className="block font-mono text-xs text-primary tracking-[0.12em] bg-white border border-primary/20 px-2.5 py-1 rounded-lg">
                                          {access.roomPassword}
                                        </strong>
                                      ) : (
                                        <span className="inline-flex items-center gap-1 px-2.5 py-1 text-[8.5px] font-extrabold text-rose-600 bg-rose-50 border border-rose-100 uppercase tracking-wider rounded-lg">
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
                            <div className="bg-white border border-slate-100 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.015)] p-6 flex flex-col gap-4">
                              <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
                                <span className="material-symbols-outlined text-primary text-base">rate_review</span>
                                <h4 className="text-xs font-black text-slate-800 uppercase tracking-widest m-0">Yêu cầu đặc biệt</h4>
                              </div>
                              <p className="text-xs text-slate-600 font-semibold bg-slate-50 p-4 border border-slate-100 rounded-xl m-0 italic flex-1 flex items-center justify-center text-center">
                                "{selectedBooking.specialRequests}"
                              </p>
                            </div>
                          )}
                        </div>
                      )}
                    </div>



                    {/* Right column - Invoice summary */}
                    <div className="bg-white border border-slate-100 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.015)] p-6 md:p-8 flex flex-col justify-between h-fit space-y-6">
                      <div>
                        <h4 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b border-slate-100 pb-3 mb-4">Chi tiết hóa đơn</h4>
                        <div className="space-y-3 text-xs text-slate-600 font-semibold">
                          <div className="flex justify-between">
                            <span>Tổng tiền phòng:</span>
                            <span className="text-slate-800 font-mono">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedBooking.totalAmount || 0)}</span>
                          </div>
                          <div className="flex justify-between">
                            <span>Phí dịch vụ phụ thu:</span>
                            <span className="text-slate-800 font-mono">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedBooking.serviceChargeAmount || 0)}</span>
                          </div>
                          <div className="flex justify-between">
                            <span>Thuế VAT (10%):</span>
                            <span className="text-slate-800 font-mono">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedBooking.taxAmount || 0)}</span>
                          </div>
                          <div className="flex justify-between border-t border-slate-100 pt-3 text-sm font-black">
                            <span className="text-slate-800">Tổng cộng:</span>
                            <span className="text-primary font-mono">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedBooking.finalAmount || selectedBooking.totalAmount || 0)}</span>
                          </div>
                          <div className="flex justify-between text-emerald-600 border-t border-slate-100 pt-2">
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
                        <span className="block text-[9px] text-slate-400 uppercase font-black tracking-widest">Thao tác nghiệp vụ:</span>
                        <div className="flex flex-col gap-2">
                          {selectedBooking.status !== 'Cancelled' && selectedBooking.status !== 'Completed' && selectedBooking.status !== 'Checked Out' && selectedBooking.status !== 'Checked-out' && (
                            <button
                              onClick={() => {
                                setEditBookingFormData({
                                  roomTypeId: selectedBooking.roomTypeId || '',
                                  checkInDate: selectedBooking.checkInDate || '',
                                  checkOutDate: selectedBooking.checkOutDate || '',
                                  quantity: selectedBooking.quantity || 1,
                                  numberOfAdults: selectedBooking.numberOfAdults || 1,
                                  numberOfChildren: selectedBooking.numberOfChildren || 0,
                                  specialRequests: selectedBooking.specialRequests || '',
                                  discountAmount: selectedBooking.discountAmount || 0,
                                });
                                setIsEditBookingModalOpen(true);
                              }}
                              className="w-full py-3 bg-blue-500/10 border border-blue-500/20 text-blue-600 hover:bg-blue-500/20 text-[10px] font-black uppercase tracking-widest cursor-pointer flex items-center justify-center gap-1.5 rounded-xl transition-all shadow-sm backdrop-blur-md"
                            >
                              <span className="material-symbols-outlined text-sm">edit</span> Chỉnh sửa đặt phòng
                            </button>
                          )}
                          <button
                            onClick={() => handleViewInvoice(selectedBooking.id)}
                            className="w-full py-3 bg-slate-50 border border-slate-200/60 hover:bg-slate-100 text-slate-700 text-[10px] font-black uppercase tracking-widest cursor-pointer flex items-center justify-center gap-1.5 rounded-xl transition-all"
                          >
                            <span className="material-symbols-outlined text-sm">receipt_long</span> Xem hóa đơn chi tiết
                          </button>
                          <button
                            onClick={() => handleDownloadPdf(selectedBooking.id, selectedBooking.bookingReference)}
                            className="w-full py-3 bg-slate-50 border border-slate-200/60 hover:bg-slate-100 text-slate-700 text-[10px] font-black uppercase tracking-widest cursor-pointer flex items-center justify-center gap-1.5 rounded-xl transition-all"
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
        <div className="fixed inset-0 bg-black/65 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white border border-slate-100 max-w-md w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl animate-scale-in text-slate-800 text-left font-['Montserrat'] rounded-2xl">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">CẬP NHẬT PHÒNG VẬT LÝ</span>
                <h4 className="text-sm font-black uppercase text-slate-800 m-0 mt-0.5">Phòng {editingRoomItem?.roomNumber}</h4>
              </div>
              <button
                onClick={() => setIsRoomEditOpen(false)}
                className="text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg p-1 transition-all border-none bg-transparent cursor-pointer flex items-center"
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
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                  required
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Tầng</label>
                <input
                  type="number"
                  value={roomEditFormData.floorNumber}
                  onChange={(e) => setRoomEditFormData(prev => ({ ...prev, floorNumber: e.target.value }))}
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                  required
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Hạng phòng (Loại phòng)</label>
                <select
                  value={roomEditFormData.roomTypeId}
                  onChange={(e) => setRoomEditFormData(prev => ({ ...prev, roomTypeId: e.target.value }))}
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all [&>option]:bg-white [&>option]:text-slate-800"
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
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all placeholder:text-slate-400"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Trạng thái phòng</label>
                <select
                  value={roomEditFormData.status}
                  onChange={(e) => setRoomEditFormData(prev => ({ ...prev, status: e.target.value }))}
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all [&>option]:bg-white [&>option]:text-slate-800"
                  required
                >
                  <option value="Available">Available</option>
                  <option value="Occupied">Occupied</option>
                  <option value="Cleaning">Cleaning</option>
                  <option value="Maintenance">Maintenance</option>
                </select>
              </div>

              <div className="flex gap-4 pt-4 border-t border-slate-100">
                <button
                  type="submit"
                  disabled={isSubmittingRoomEdit}
                  className="bg-primary hover:brightness-110 text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 flex-1 rounded-xl shadow-[0_4px_12px_rgba(162,5,19,0.2)] disabled:opacity-60"
                >
                  {isSubmittingRoomEdit ? 'Đang lưu...' : 'Lưu thay đổi'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsRoomEditOpen(false)}
                  className="bg-slate-50 hover:bg-slate-100 text-slate-700 font-bold px-8 py-3.5 uppercase text-xs tracking-widest border border-slate-200/60 cursor-pointer flex-1 rounded-xl transition-all"
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
        <div className="fixed inset-0 bg-neutral-950/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white border border-slate-100 max-w-2xl w-full p-6 md:p-8 flex flex-col gap-6 shadow-[0_8px_30px_rgb(0,0,0,0.06)] rounded-2xl animate-scale-in text-slate-800 text-left">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">Hệ thống lễ tân</span>
                <h4 className="text-sm font-black uppercase text-slate-800 m-0 mt-0.5">Đặt phòng Walk-in trực tiếp</h4>
              </div>
              <button
                onClick={() => setIsWalkInModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 border-none bg-transparent cursor-pointer flex items-center transition-colors"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <form onSubmit={handleWalkInSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* Room type selection */}
                <div className="space-y-1.5">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Loại phòng *</label>
                  <select
                    value={walkInFormData.roomTypeId}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, roomTypeId: e.target.value }))}
                    className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all [&>option]:bg-white [&>option]:text-slate-800"
                    required
                  >
                    <option value="">Chọn loại phòng</option>
                    {roomTypes.map(type => (
                      <option key={type.id} value={type.id}>{type.name} - {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(type.basePrice || type.baseprice)}</option>
                    ))}
                  </select>
                </div>

                <div className="space-y-1.5">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Số lượng phòng *</label>
                  <input
                    type="number"
                    min="1"
                    value={walkInFormData.quantity || 1}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, quantity: e.target.value }))}
                    className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Ngày nhận phòng (Check-in) *</label>
                  <input
                    type="date"
                    value={walkInFormData.checkInDate}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, checkInDate: e.target.value }))}
                    className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                    required
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Ngày trả phòng (Check-out) *</label>
                  <input
                    type="date"
                    value={walkInFormData.checkOutDate}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, checkOutDate: e.target.value }))}
                    className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Người lớn *</label>
                  <input
                    type="number"
                    min="1"
                    value={walkInFormData.numberOfAdults || 1}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, numberOfAdults: e.target.value }))}
                    className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                    required
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Trẻ em</label>
                  <input
                    type="number"
                    min="0"
                    value={walkInFormData.numberOfChildren || 0}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, numberOfChildren: e.target.value }))}
                    className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                  />
                </div>
              </div>

              <div className="border-t border-slate-100 pt-4">
                <span className="text-[10px] font-black tracking-widest text-primary uppercase block mb-3">Thông tin khách hàng</span>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Họ và tên *</label>
                    <input
                      type="text"
                      value={walkInFormData.customerFullname}
                      onChange={(e) => setWalkInFormData(prev => ({ ...prev, customerFullname: e.target.value }))}
                      placeholder="Nguyễn Văn A"
                      className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all placeholder:text-slate-400"
                      required
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Email *</label>
                    <input
                      type="email"
                      value={walkInFormData.customerEmail}
                      onChange={(e) => setWalkInFormData(prev => ({ ...prev, customerEmail: e.target.value }))}
                      placeholder="email@example.com"
                      className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all placeholder:text-slate-400"
                      required
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Số điện thoại *</label>
                    <input
                      type="tel"
                      value={walkInFormData.customerPhonenumber}
                      onChange={(e) => setWalkInFormData(prev => ({ ...prev, customerPhonenumber: e.target.value }))}
                      placeholder="0901234567"
                      className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all placeholder:text-slate-400"
                      required
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Số CCCD *</label>
                    <input
                      type="text"
                      inputMode="numeric"
                      maxLength={12}
                      value={walkInFormData.customerIdCardNumber}
                      onChange={(e) => setWalkInFormData(prev => ({ ...prev, customerIdCardNumber: e.target.value }))}
                      placeholder="012345678901"
                      className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all placeholder:text-slate-400"
                      required
                    />
                  </div>
                </div>
              </div>

              <div className="border-t border-slate-100 pt-4 grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Số tiền đóng trước (đ)</label>
                  <input
                    type="number"
                    value={walkInFormData.paidAmount}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, paidAmount: e.target.value }))}
                    placeholder="0"
                    className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all placeholder:text-slate-400"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Phương thức thanh toán</label>
                  <select
                    value={walkInFormData.paymentMethod}
                    onChange={(e) => setWalkInFormData(prev => ({ ...prev, paymentMethod: e.target.value }))}
                    className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all [&>option]:bg-white [&>option]:text-slate-800"
                  >
                    <option value="Cash">Tiền mặt (Cash)</option>
                    <option value="Card">Thẻ (Card)</option>
                    <option value="Transfer">Chuyển khoản (Transfer)</option>
                  </select>
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Yêu cầu đặc biệt</label>
                <textarea
                  value={walkInFormData.specialRequests}
                  onChange={(e) => setWalkInFormData(prev => ({ ...prev, specialRequests: e.target.value }))}
                  placeholder="Yêu cầu khác..."
                  rows="2"
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl p-3 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all resize-none placeholder:text-slate-400"
                />
              </div>

              <div className="flex gap-4 pt-4 border-t border-slate-100">
                <button
                  type="submit"
                  disabled={isSubmittingWalkIn}
                  className="bg-primary text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest hover:brightness-110 transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 rounded-xl flex-1 shadow-[0_4px_12px_rgba(162,5,19,0.2)]"
                >
                  {isSubmittingWalkIn ? 'Đang đặt phòng...' : 'Xác nhận Đặt & Check-in'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsWalkInModalOpen(false)}
                  className="bg-slate-100 hover:bg-slate-200 border border-slate-200/60 text-slate-700 font-bold px-8 py-3.5 uppercase text-xs tracking-widest cursor-pointer rounded-xl flex-1 transition-all"
                >
                  Hủy bỏ
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* EDIT BOOKING FORM MODAL */}
      {isEditBookingModalOpen && (
        <div className="fixed inset-0 bg-neutral-950/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white border border-slate-100 max-w-2xl w-full p-6 md:p-8 flex flex-col gap-6 shadow-[0_8px_30px_rgb(0,0,0,0.06)] rounded-2xl animate-scale-in text-slate-800 text-left">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">Chi tiết lưu trú</span>
                <h4 className="text-sm font-black uppercase text-slate-800 m-0 mt-0.5">Chỉnh sửa đơn đặt phòng</h4>
              </div>
              <button
                onClick={() => setIsEditBookingModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 border-none bg-transparent cursor-pointer flex items-center transition-colors"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <form onSubmit={handleEditBookingSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* Room type selection */}
                <div className="space-y-1.5">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Loại phòng *</label>
                  <select
                    value={editBookingFormData.roomTypeId}
                    onChange={(e) => setEditBookingFormData(prev => ({ ...prev, roomTypeId: e.target.value }))}
                    className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all [&>option]:bg-white [&>option]:text-slate-800"
                    required
                  >
                    <option value="">Chọn loại phòng</option>
                    {roomTypes.map(type => (
                      <option key={type.id} value={type.id}>{type.name} - {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(type.basePrice || type.baseprice)}</option>
                    ))}
                  </select>
                </div>

                <div className="space-y-1.5">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Số lượng phòng *</label>
                  <input
                    type="number"
                    min="1"
                    value={editBookingFormData.quantity || 1}
                    onChange={(e) => setEditBookingFormData(prev => ({ ...prev, quantity: e.target.value }))}
                    className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Ngày nhận phòng (Check-in) *</label>
                  <input
                    type="date"
                    value={editBookingFormData.checkInDate}
                    onChange={(e) => setEditBookingFormData(prev => ({ ...prev, checkInDate: e.target.value }))}
                    className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                    required
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Ngày trả phòng (Check-out) *</label>
                  <input
                    type="date"
                    value={editBookingFormData.checkOutDate}
                    onChange={(e) => setEditBookingFormData(prev => ({ ...prev, checkOutDate: e.target.value }))}
                    className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="space-y-1.5">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Người lớn *</label>
                  <input
                    type="number"
                    min="1"
                    value={editBookingFormData.numberOfAdults || 1}
                    onChange={(e) => setEditBookingFormData(prev => ({ ...prev, numberOfAdults: e.target.value }))}
                    className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                    required
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Trẻ em *</label>
                  <input
                    type="number"
                    min="0"
                    value={editBookingFormData.numberOfChildren || 0}
                    onChange={(e) => setEditBookingFormData(prev => ({ ...prev, numberOfChildren: e.target.value }))}
                    className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                    required
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Tiền giảm giá (VND)</label>
                  <input
                    type="number"
                    min="0"
                    value={editBookingFormData.discountAmount || 0}
                    onChange={(e) => setEditBookingFormData(prev => ({ ...prev, discountAmount: e.target.value }))}
                    className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">Yêu cầu đặc biệt</label>
                <textarea
                  value={editBookingFormData.specialRequests || ''}
                  onChange={(e) => setEditBookingFormData(prev => ({ ...prev, specialRequests: e.target.value }))}
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all min-h-[80px]"
                  placeholder="Nhập yêu cầu đặc biệt..."
                />
              </div>

              <div className="flex gap-4 pt-4 border-t border-slate-100">
                <button
                  type="submit"
                  disabled={isSubmittingEditBooking}
                  className="bg-primary text-white font-black px-8 py-3.5 uppercase text-xs tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 flex-1 rounded-xl shadow-[0_4px_12px_rgba(162,5,19,0.2)]"
                >
                  {isSubmittingEditBooking ? 'Đang lưu...' : 'Lưu thay đổi'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsEditBookingModalOpen(false)}
                  className="bg-slate-50 hover:bg-slate-100 border border-slate-200/60 text-slate-700 font-bold px-8 py-3.5 uppercase text-xs tracking-widest cursor-pointer flex-1 rounded-xl transition-all"
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
          <div className="bg-white border border-slate-200/80 max-w-2xl w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl rounded-2xl animate-scale-in text-slate-800 text-left font-['Montserrat'] my-8">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">HÓA ĐƠN CHI TIẾT</span>
                <h4 className="text-sm font-black uppercase text-slate-800 m-0 mt-0.5">
                  {invoiceLoading ? 'Đang tải...' : `Mã đơn: ${invoiceData?.bookingReference}`}
                </h4>
              </div>
              <button
                onClick={() => setIsInvoiceModalOpen(false)}
                className="text-slate-400 hover:text-slate-650 border-none bg-transparent cursor-pointer flex items-center transition-all"
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
              <div className="space-y-6 text-xs text-slate-650">
                {/* Guest info */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 border-b border-slate-100 pb-4">
                  <div>
                    <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Khách hàng</span>
                    <strong className="text-slate-800 text-sm block mt-1 uppercase font-black">{invoiceData.customerName}</strong>
                    <span className="block mt-0.5">{invoiceData.customerEmail} • {invoiceData.customerPhone}</span>
                  </div>
                  <div>
                    <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block">Thời gian lưu trú</span>
                    <strong className="text-slate-800 block mt-1">{invoiceData.checkInDate} đến {invoiceData.checkOutDate}</strong>
                    <span className="block mt-0.5">Tổng cộng: {invoiceData.nights} đêm • Hạng phòng: {invoiceData.roomTypeName}</span>
                  </div>
                </div>

                {/* Calculation details */}
                <div className="space-y-3">
                  <div className="flex justify-between border-b border-slate-100 pb-2">
                    <span className="font-bold uppercase tracking-wider text-slate-400">Diễn giải dịch vụ</span>
                    <span className="font-bold uppercase tracking-wider text-slate-400">Thành tiền</span>
                  </div>

                  {/* Room Charge */}
                  <div className="flex justify-between text-slate-800 font-medium">
                    <div>
                      <span>Tiền phòng ({invoiceData.quantity || 1} phòng x {invoiceData.nights} đêm)</span>
                      <span className="block text-[10px] text-slate-500 font-semibold">Đơn giá: {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(invoiceData.roomRate)}</span>
                    </div>
                    <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(invoiceData.roomTotal)}</span>
                  </div>

                  {/* Service charges */}
                  {invoiceData.services && invoiceData.services.length > 0 && (
                    <div className="space-y-2 border-t border-slate-100 pt-3">
                      <span className="block text-[9px] font-black text-slate-500 uppercase tracking-widest">Dịch vụ phụ trội</span>
                      {invoiceData.services.map((svc, idx) => (
                        <div key={idx} className="flex justify-between font-medium text-slate-800">
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
                <div className="border-t border-slate-100 pt-4 space-y-2">
                  <div className="flex justify-between text-slate-500 font-bold">
                    <span>Tổng chưa thuế:</span>
                    <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(parseFloat(invoiceData.roomTotal) + parseFloat(invoiceData.serviceTotal || 0))}</span>
                  </div>
                  <div className="flex justify-between text-slate-500 font-bold">
                    <span>Thuế VAT (10%):</span>
                    <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(invoiceData.taxAmount)}</span>
                  </div>
                  {parseFloat(invoiceData.discountAmount || 0) > 0 && (
                    <div className="flex justify-between text-rose-600 font-bold">
                      <span>Giảm giá:</span>
                      <span>-{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(invoiceData.discountAmount)}</span>
                    </div>
                  )}
                  <div className="flex justify-between border-t border-slate-100 pt-3 items-center">
                    <span className="font-black text-slate-800 uppercase tracking-wider text-xs">TỔNG CỘNG HÓA ĐƠN:</span>
                    <span className="font-black text-primary text-base">
                      {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(invoiceData.finalAmount)}
                    </span>
                  </div>
                  <div className="flex justify-between text-emerald-600 font-bold items-center">
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
                              background: 'linear-gradient(135deg, #10b981, #059669)',
                              color: '#fff', fontSize: '9px', fontWeight: 900,
                              letterSpacing: '0.08em', padding: '2px 8px',
                              borderRadius: '3px', textTransform: 'uppercase',
                              boxShadow: '0 0 8px rgba(16,185,129,0.3)'
                            }}>
                              ✓ Thanh toán 100%
                            </span>
                          );
                        } else if (isDeposit) {
                          return (
                            <span style={{
                              display: 'inline-flex', alignItems: 'center', gap: '4px',
                              background: 'linear-gradient(135deg, #f59e0b, #d97706)',
                              color: '#fff', fontSize: '9px', fontWeight: 900,
                              letterSpacing: '0.08em', padding: '2px 8px',
                              borderRadius: '3px', textTransform: 'uppercase',
                              boxShadow: '0 0 8px rgba(245,158,11,0.3)'
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
                  <div className="flex justify-between border-t border-slate-100 pt-2 text-xs font-black text-slate-800">
                    <span>CÒN LẠI PHẢI THANH TOÁN (DUE):</span>
                    <span className={parseFloat(invoiceData.dueAmount) > 0 ? "text-rose-600" : "text-emerald-600"}>
                      {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(invoiceData.dueAmount)}
                    </span>
                  </div>
                </div>

                {/* Payments breakdown */}
                {invoiceData.payments && invoiceData.payments.length > 0 && (
                  <div className="border-t border-slate-100 pt-4">
                    <span className="block text-[9px] font-black text-slate-500 uppercase tracking-widest mb-2">Lịch sử thanh toán</span>
                    <div className="space-y-2">
                      {invoiceData.payments.map((pmt, idx) => (
                        <div key={idx} className="bg-slate-50 p-3 border border-slate-200/60 rounded-xl flex justify-between items-center text-[10px] text-slate-700">
                          <div>
                            <span className="block font-black text-slate-800 uppercase">{pmt.paymentType === 'Advance' ? 'Đặt cọc' : 'Thanh toán'} • {pmt.paymentMethod}</span>
                            <span className="block text-[9px] text-slate-500 mt-0.5">Mã giao dịch: {pmt.transactionCode || 'N/A'} • Ngày: {new Date(pmt.paymentDate).toLocaleString('vi-VN')}</span>
                          </div>
                          <div className="text-right">
                            <span className="block font-black text-slate-800">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(pmt.amount)}</span>
                            {parseFloat(pmt.refundedAmount || 0) > 0 && (
                              <span className="block text-[9px] text-rose-600 font-semibold">Đã hoàn: -{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(pmt.refundedAmount)}</span>
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
                    <div className="border-t border-slate-100 pt-4 space-y-3">
                      <span className="block text-[9px] font-black text-slate-500 uppercase tracking-widest">
                        Thêm dịch vụ phụ thu (Minibar, Spa, Concierge...)
                      </span>
                      <form onSubmit={handleInlineServiceSubmit} className="grid grid-cols-1 sm:grid-cols-4 gap-3 bg-slate-50 p-4 border border-slate-200/60 rounded-xl">
                        <div className="sm:col-span-2">
                          <label className="block text-[9px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                            Chọn dịch vụ
                          </label>
                          <select
                            value={inlineServiceId}
                            onChange={(e) => setInlineServiceId(e.target.value)}
                            className="w-full bg-white border border-slate-200 text-slate-800 text-xs px-2.5 py-2.5 rounded-xl focus:border-primary outline-none transition-all"
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
                            className="w-full bg-white border border-slate-200 text-slate-800 text-xs px-2.5 py-2 rounded-xl focus:border-primary outline-none transition-all"
                            required
                          />
                        </div>
                        <div className="flex items-end">
                          <button
                            type="submit"
                            disabled={isSubmittingInlineService}
                            className="w-full bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-700 text-[10px] font-black uppercase tracking-widest py-2.5 px-3 cursor-pointer flex items-center justify-center gap-1 rounded-xl transition-all"
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
                            className="w-full bg-white border border-slate-200 text-slate-800 text-xs px-2.5 py-2 rounded-xl focus:border-primary outline-none transition-all"
                          />
                        </div>
                      </form>
                    </div>
                  );
                })()}

                {/* Counter Payment Form */}
                {parseFloat(invoiceData.dueAmount) > 0 && (
                  <div className="border-t border-slate-100 pt-4 space-y-3">
                    <span className="block text-[9px] font-black text-slate-500 uppercase tracking-widest">
                      Ghi nhận thanh toán tại quầy
                    </span>
                    <form onSubmit={handleManualPaymentSubmit} className="grid grid-cols-1 sm:grid-cols-3 gap-3 bg-slate-50 p-4 border border-slate-200/60 rounded-xl">
                      <div>
                        <label className="block text-[9px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                          Phương thức
                        </label>
                        <select
                          value={manualPaymentMethod}
                          onChange={(e) => setManualPaymentMethod(e.target.value)}
                          className="w-full bg-white border border-slate-200 text-slate-800 text-xs px-2.5 py-2.5 rounded-xl focus:border-primary outline-none transition-all"
                        >
                          <option value="Cash">Tiền mặt (Cash)</option>
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
                          className="w-full bg-white border border-slate-200 text-slate-800 text-xs px-2.5 py-2 rounded-xl focus:border-primary outline-none transition-all"
                          required
                        />
                      </div>
                      <div className="flex items-end">
                        <button
                          type="submit"
                          disabled={isSubmittingManualPayment}
                          className="w-full bg-primary hover:brightness-110 disabled:bg-slate-100 disabled:text-slate-400 disabled:cursor-not-allowed text-white text-[10px] font-black uppercase tracking-widest py-2.5 px-3 border-none cursor-pointer flex items-center justify-center gap-1 rounded-xl transition-all shadow-[0_4px_12px_rgba(162,5,19,0.15)]"
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
                          className="w-full bg-white border border-slate-200 text-slate-800 text-xs px-2.5 py-2 rounded-xl focus:border-primary outline-none transition-all"
                        />
                      </div>
                    </form>
                  </div>
                )}

                <div className="flex flex-wrap gap-2 justify-end pt-4 border-t border-slate-100">
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
                            className="bg-primary hover:brightness-110 text-white font-black px-6 py-3 uppercase text-[10px] tracking-widest cursor-pointer border-none flex items-center gap-1.5 rounded-xl transition-all shadow-[0_4px_12px_rgba(162,5,19,0.2)]"
                          >
                            <span className="material-symbols-outlined text-xs">done_all</span> Hoàn tất Checkout
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => handleDownloadPdf(invoiceData.bookingId, invoiceData.bookingReference)}
                          className="bg-slate-100 hover:bg-slate-200 border border-slate-200/80 text-slate-700 font-black px-6 py-3 uppercase text-[10px] tracking-widest cursor-pointer flex items-center gap-1.5 rounded-xl transition-all"
                        >
                          <span className="material-symbols-outlined text-xs">download</span> Xuất PDF
                        </button>

                      </>
                    );
                  })()}
                  <button
                    onClick={() => setIsInvoiceModalOpen(false)}
                    className="bg-slate-100 hover:bg-slate-200 border border-slate-200/80 text-slate-700 font-bold px-6 py-3 uppercase text-[10px] tracking-widest cursor-pointer rounded-xl transition-all"
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
                <label className="block text-[9px] font-bold text-slate-500 uppercase tracking-widest">Số lượng</label>
                <input
                  type="number"
                  min="1"
                  value={serviceQuantity}
                  onChange={(e) => setServiceQuantity(parseInt(e.target.value) || 1)}
                  className="w-full bg-white border border-slate-200/80 text-slate-800 text-xs px-3 py-2.5 focus:border-primary outline-none rounded-xl"
                  required
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[9px] font-bold text-slate-500 uppercase tracking-widest">Ghi chú</label>
                <input
                  type="text"
                  value={serviceNote}
                  onChange={(e) => setServiceNote(e.target.value)}
                  placeholder="Ví dụ: Khách gọi thêm từ minibar"
                  className="w-full bg-white border border-slate-200/80 text-slate-800 text-xs px-3 py-2.5 focus:border-primary outline-none rounded-xl"
                />
              </div>

              <div className="flex gap-4 pt-4 border-t border-slate-100">
                <button
                  type="submit"
                  disabled={isSubmittingService}
                  className="bg-primary text-white font-bold px-6 py-3 uppercase text-xs tracking-widest hover:brightness-110 transition-all cursor-pointer border-none flex-1 flex items-center justify-center gap-1.5 rounded-xl"
                >
                  {isSubmittingService ? 'Đang lưu...' : 'Thêm dịch vụ'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsAddServiceModalOpen(false)}
                  className="bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-700 font-bold px-6 py-3 uppercase text-xs tracking-widest cursor-pointer flex-1 rounded-xl transition-all"
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
        <div className="fixed inset-0 bg-neutral-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 font-['Montserrat']">
          <div className="bg-white border border-slate-200/80 max-w-md w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl rounded-2xl animate-scale-in text-slate-800 text-left">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-emerald-600 uppercase">PHÊ DUYỆT HOÀN TIỀN</span>
                <h4 className="text-sm font-black uppercase text-slate-900 m-0 mt-0.5">{selectedRefund.bookingReference}</h4>
              </div>
              <button
                onClick={() => setIsApproveRefundOpen(false)}
                className="text-slate-400 hover:text-slate-700 border-none bg-transparent cursor-pointer flex items-center transition-all"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <form onSubmit={handleApproveRefundSubmit} className="space-y-4">
              <div className="bg-slate-50 border border-slate-200/80 p-4 rounded-xl space-y-2.5 text-xs">
                <div className="flex justify-between gap-4">
                  <span className="text-slate-500 uppercase font-bold text-[9px] shrink-0">Lý do từ khách:</span>
                  <span className="text-slate-800 font-semibold italic text-right">"{selectedRefund.description || 'Không có lý do'}"</span>
                </div>
                <div className="flex justify-between border-t border-slate-200/60 pt-2">
                  <span className="text-slate-500 uppercase font-bold text-[9px]">Số tiền ban đầu:</span>
                  <span className="text-slate-900 font-bold font-mono">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(parseFloat(selectedRefund.oldValue || 0))}</span>
                </div>
                <div className="flex justify-between border-t border-slate-200/60 pt-1.5">
                  <span className="text-emerald-600 uppercase font-bold text-[9px]">Hoàn tiền dự kiến:</span>
                  <strong className="text-emerald-600 font-mono text-xs">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(parseFloat(selectedRefund.newValue || 0))}</strong>
                </div>
              </div>

              {parseFloat(selectedRefund.newValue || 0) === 0 && (
                <div className="p-3.5 bg-amber-50 border border-amber-200/80 rounded-xl text-amber-800 text-[11px] font-semibold leading-relaxed flex items-start gap-2">
                  <span className="material-symbols-outlined text-base text-amber-600 shrink-0 mt-0.5">warning</span>
                  <div>
                    <strong className="font-bold text-amber-900 block uppercase tracking-wider text-[9.5px] mb-0.5">Cảnh báo hủy sát ngày (0% Hoàn tiền):</strong>
                    Đơn hàng này được yêu cầu hủy sát ngày/trong ngày Check-in. Theo chính sách khách sạn, tỷ lệ hoàn tiền tự động là <strong>0 VNĐ</strong>. Bạn có thể giữ 0 VNĐ hoặc điều chỉnh số tiền muốn hỗ trợ hoàn cho khách.
                  </div>
                </div>
              )}

              <div className="space-y-1.5">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">
                  Số tiền hoàn trả thực tế (VND):
                </label>
                <input
                  type="number"
                  value={refundOverrideAmount}
                  onChange={(e) => setRefundOverrideAmount(e.target.value)}
                  placeholder="Nhập số tiền hoàn trả..."
                  className="w-full bg-slate-50 border border-slate-200/80 p-3 font-bold text-xs outline-none text-slate-900 focus:border-emerald-500 focus:bg-white rounded-xl transition-all"
                  required
                />
                <span className="text-[9px] text-slate-400 uppercase block leading-relaxed">
                  *Để trống hoặc nhập số tiền khác để ghi đè. Không được vượt quá số tiền ban đầu.
                </span>
              </div>

              <div className="flex gap-4 pt-4 border-t border-slate-100">
                <button
                  type="submit"
                  disabled={isSubmittingRefundAction}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white font-black px-8 py-3.5 uppercase text-xs tracking-widest transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 flex-1 rounded-xl shadow-sm"
                >
                  {isSubmittingRefundAction ? 'Đang xử lý...' : 'Xác nhận duyệt'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsApproveRefundOpen(false)}
                  className="bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-700 font-bold px-8 py-3.5 uppercase text-xs tracking-widest cursor-pointer flex-1 rounded-xl transition-all"
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
        <div className="fixed inset-0 bg-neutral-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 font-['Montserrat']">
          <div className="bg-white border border-slate-200/80 max-w-md w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl rounded-2xl animate-scale-in text-slate-800 text-left">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-rose-600 uppercase">TỪ CHỐI HOÀN TIỀN</span>
                <h4 className="text-sm font-black uppercase text-slate-900 m-0 mt-0.5">{selectedRefund.bookingReference}</h4>
              </div>
              <button
                onClick={() => setIsRejectRefundOpen(false)}
                className="text-slate-400 hover:text-slate-700 border-none bg-transparent cursor-pointer flex items-center transition-all"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <form onSubmit={handleRejectRefundSubmit} className="space-y-4">
              <div className="space-y-1.5">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">
                  Lý do từ chối yêu cầu:
                </label>
                <textarea
                  rows="4"
                  value={refundRejectionReason}
                  onChange={(e) => setRefundRejectionReason(e.target.value)}
                  placeholder="Vui lòng cung cấp lý do từ chối cụ thể..."
                  className="w-full bg-slate-50 border border-slate-200/80 p-3 font-bold text-xs outline-none text-slate-900 focus:border-rose-500 focus:bg-white resize-none rounded-xl transition-all"
                  required
                />
              </div>

              <div className="flex gap-4 pt-4 border-t border-slate-100">
                <button
                  type="submit"
                  disabled={isSubmittingRefundAction}
                  className="bg-rose-600 hover:bg-rose-700 text-white font-black px-8 py-3.5 uppercase text-xs tracking-widest transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 flex-1 rounded-xl shadow-sm"
                >
                  {isSubmittingRefundAction ? 'Đang xử lý...' : 'Xác nhận từ chối'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsRejectRefundOpen(false)}
                  className="bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-700 font-bold px-8 py-3.5 uppercase text-xs tracking-widest cursor-pointer flex-1 rounded-xl transition-all"
                >
                  Hủy bỏ
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* REJECT ROOM CHANGE MODAL */}
      {isRejectRoomChangeOpen && selectedRoomChange && (
        <div className="fixed inset-0 bg-neutral-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 font-['Montserrat']">
          <div className="bg-white border border-slate-200/80 max-w-md w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl rounded-2xl animate-scale-in text-slate-800 text-left">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">TỪ CHỐI CHUYỂN PHÒNG</span>
                <h4 className="text-sm font-black uppercase text-slate-900 m-0 mt-0.5">{selectedRoomChange.bookingReference}</h4>
              </div>
              <button
                onClick={() => setIsRejectRoomChangeOpen(false)}
                className="text-slate-400 hover:text-slate-700 border-none bg-transparent cursor-pointer flex items-center transition-all"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <form onSubmit={(e) => { e.preventDefault(); handleRejectRoomChange(); }} className="space-y-4">
              <div className="space-y-1.5">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">
                  Lý do từ chối yêu cầu đổi phòng:
                </label>
                <textarea
                  rows="4"
                  value={roomChangeRejectionReason}
                  onChange={(e) => setRoomChangeRejectionReason(e.target.value)}
                  placeholder="Vui lòng cung cấp lý do từ chối cụ thể..."
                  className="w-full bg-slate-50 border border-slate-200/80 p-3 font-bold text-xs outline-none text-slate-900 focus:border-primary focus:bg-white resize-none rounded-xl transition-all"
                  required
                />
              </div>

              <div className="flex gap-4 pt-4 border-t border-slate-100">
                <button
                  type="submit"
                  disabled={isSubmittingRoomChangeAction}
                  className="bg-rose-600 hover:bg-rose-700 text-white font-black px-8 py-3.5 uppercase text-xs tracking-widest transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 flex-1 rounded-xl shadow-sm"
                >
                  {isSubmittingRoomChangeAction ? 'Đang xử lý...' : 'Xác nhận từ chối'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsRejectRoomChangeOpen(false)}
                  className="bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-700 font-bold px-8 py-3.5 uppercase text-xs tracking-widest cursor-pointer flex-1 rounded-xl transition-all"
                >
                  Hủy bỏ
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* REJECT STAY EXTENSION MODAL */}
      {isRejectStayExtensionOpen && selectedStayExtension && (
        <div className="fixed inset-0 bg-neutral-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 font-['Montserrat']">
          <div className="bg-white border border-slate-200/80 max-w-md w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl rounded-2xl animate-scale-in text-slate-800 text-left">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">TỪ CHỐI GIA HẠN</span>
                <h4 className="text-sm font-black uppercase text-slate-900 m-0 mt-0.5">{selectedStayExtension.bookingReference}</h4>
              </div>
              <button
                onClick={() => setIsRejectStayExtensionOpen(false)}
                className="text-slate-400 hover:text-slate-700 border-none bg-transparent cursor-pointer flex items-center transition-all"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <form onSubmit={(e) => { e.preventDefault(); handleRejectStayExtension(); }} className="space-y-4">
              <div className="space-y-1.5">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">
                  Lý do từ chối gia hạn:
                </label>
                <textarea
                  rows="4"
                  value={stayExtensionRejectionReason}
                  onChange={(e) => setStayExtensionRejectionReason(e.target.value)}
                  placeholder="Vui lòng cung cấp lý do từ chối cụ thể..."
                  className="w-full bg-transparent border border-neutral-855 p-3 font-bold text-xs outline-none text-white focus:border-primary resize-none"
                  required
                />
              </div>

              <div className="flex gap-4 pt-4 border-t border-neutral-900">
                <button
                  type="submit"
                  disabled={isSubmittingStayExtensionAction}
                  className="bg-[#e11d48] text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest hover:brightness-110 transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 flex-1"
                >
                  {isSubmittingStayExtensionAction ? 'Đang xử lý...' : 'Xác nhận từ chối'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsRejectStayExtensionOpen(false)}
                  className="bg-neutral-900 hover:bg-neutral-850 border border-neutral-855 text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest cursor-pointer flex-1"
                >
                  Hủy bỏ
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* REJECT EARLY CHECKOUT MODAL */}
      {isRejectEarlyCheckOutOpen && selectedEarlyCheckOut && (
        <div className="fixed inset-0 bg-neutral-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 font-['Montserrat']">
          <div className="bg-white border border-slate-200/80 max-w-md w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl rounded-2xl animate-scale-in text-slate-800 text-left">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">TỪ CHỐI CHECK-OUT SỚM</span>
                <h4 className="text-sm font-black uppercase text-slate-900 m-0 mt-0.5">{selectedEarlyCheckOut.bookingReference}</h4>
              </div>
              <button
                onClick={() => setIsRejectEarlyCheckOutOpen(false)}
                className="text-slate-400 hover:text-slate-700 border-none bg-transparent cursor-pointer flex items-center transition-all"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <form onSubmit={(e) => { e.preventDefault(); handleRejectEarlyCheckOut(); }} className="space-y-4">
              <div className="space-y-1.5">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">
                  Lý do từ chối check-out sớm:
                </label>
                <textarea
                  rows="4"
                  value={earlyCheckOutRejectionReason}
                  onChange={(e) => setEarlyCheckOutRejectionReason(e.target.value)}
                  placeholder="Vui lòng cung cấp lý do từ chối cụ thể..."
                  className="w-full bg-slate-50 border border-slate-200/80 p-3 font-bold text-xs outline-none text-slate-900 focus:border-primary focus:bg-white resize-none rounded-xl transition-all"
                  required
                />
              </div>

              <div className="flex gap-4 pt-4 border-t border-slate-100">
                <button
                  type="submit"
                  disabled={isSubmittingEarlyCheckOutAction}
                  className="bg-rose-600 hover:bg-rose-700 text-white font-black px-8 py-3.5 uppercase text-xs tracking-widest transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 flex-1 rounded-xl shadow-sm"
                >
                  {isSubmittingEarlyCheckOutAction ? 'Đang xử lý...' : 'Xác nhận từ chối'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsRejectEarlyCheckOutOpen(false)}
                  className="bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-700 font-bold px-8 py-3.5 uppercase text-xs tracking-widest cursor-pointer flex-1 rounded-xl transition-all"
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
        <div className="fixed inset-0 bg-neutral-950/60 backdrop-blur-sm z-[6000] flex items-center justify-center p-4 font-['Montserrat']">
          <div className="bg-white border border-slate-200/80 max-w-sm w-full p-6 flex flex-col gap-4 shadow-2xl rounded-2xl animate-scale-in text-slate-800 text-left">
            <div className="flex items-center gap-2 text-rose-600">
              <span className="material-symbols-outlined text-lg">warning</span>
              <span className="text-[10px] font-black uppercase tracking-widest">{confirmDialog.title || "XÁC NHẬN"}</span>
            </div>
            <p className="text-xs text-slate-600 font-semibold leading-relaxed m-0">
              {confirmDialog.message}
            </p>
            <div className="flex justify-end gap-2.5 pt-2 text-[10px] font-black uppercase tracking-widest border-t border-slate-100">
              <button
                type="button"
                onClick={() => setConfirmDialog(prev => ({ ...prev, isOpen: false }))}
                className="px-4 py-2.5 border border-slate-200 text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition-all cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={() => {
                  if (confirmDialog.onConfirm) confirmDialog.onConfirm();
                  setConfirmDialog(prev => ({ ...prev, isOpen: false }));
                }}
                className="px-4 py-2.5 bg-rose-600 text-white hover:bg-rose-700 rounded-xl transition-all cursor-pointer border-none shadow-sm"
              >
                {confirmDialog.confirmText || 'Xác nhận'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
