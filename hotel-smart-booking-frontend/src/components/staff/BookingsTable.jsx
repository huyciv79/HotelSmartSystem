import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  useReactTable,
  getCoreRowModel,
  flexRender,
} from '@tanstack/react-table';
import { filterBookings, cancelBooking } from '../../services/bookingService';

const BookingsTable = ({
  showToast,
  setSelectedBooking,
  startScanner,
  handleDirectCheckInOut,
  handleViewInvoice,
  handleDownloadPdf,
  isManager,
  setActiveTab,
  onFaceCheckInSelect,
  triggerCustomConfirm,
  // Prop mới: callback mở modal chuyển phòng, nhận vào object booking
  onRoomChangeSelect,
}) => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pageCount, setPageCount] = useState(0);

  // Pagination & Filtering state
  const [pagination, setPagination] = useState({
    pageIndex: 0,
    pageSize: 10,
  });
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('');
  const [bookingType, setBookingType] = useState('');

  // Fetch function
  const fetchBookings = useCallback(async () => {
    setLoading(true);
    try {
      const criteria = {
        page: pagination.pageIndex,
        pageSize: pagination.pageSize,
        status: status || null,
        bookingType: bookingType || null,
        guestName: keyword || null,
      };

      // If keyword looks like a booking reference, set bookingReference
      if (keyword) {
        const trimmedKeyword = keyword.trim();
        if (trimmedKeyword.toUpperCase().startsWith('BK')) {
          criteria.bookingReference = trimmedKeyword;
          criteria.guestName = null;
        } else if (trimmedKeyword.includes('@')) {
          criteria.guestEmail = trimmedKeyword;
          criteria.guestName = null;
        } else {
          criteria.guestName = trimmedKeyword;
        }
      }

      const response = await filterBookings(criteria);
      if (response && response.success && response.data) {
        const mappedContent = (response.data.content || []).map(bk => ({
          ...bk,
          id: bk.bookingId,
          bookingReference: bk.bookingNumber,
          email: bk.guestEmail
        }));
        setData(mappedContent);
        setPageCount(response.data.totalPages || 0);
      }
    } catch (err) {
      console.error(err);
      showToast('Không thể tải danh sách đặt phòng', 'error');
    } finally {
      setLoading(false);
    }
  }, [pagination.pageIndex, pagination.pageSize, keyword, status, bookingType, showToast]);

  useEffect(() => {
    fetchBookings();
  }, [fetchBookings]);

  useEffect(() => {
    const handleReload = () => fetchBookings();
    window.addEventListener('reload-bookings', handleReload);
    return () => window.removeEventListener('reload-bookings', handleReload);
  }, [fetchBookings]);

  const handleCancelClick = useCallback((bk) => {
    triggerCustomConfirm(
      "Xác nhận hủy đặt phòng",
      `Bạn có chắc chắn muốn hủy đơn đặt phòng ${bk.bookingReference}? Hành động này sẽ cập nhật trạng thái và tự động thực hiện hoàn trả tiền (nếu có) ngay lập tức.`,
      async () => {
        try {
          await cancelBooking(bk.id, "Hủy trực tiếp bởi nhân viên");
          showToast(`Hủy đặt phòng ${bk.bookingReference} thành công!`, "success");
          fetchBookings();
        } catch (err) {
          console.error("Lỗi khi hủy đặt phòng:", err);
          showToast(err.response?.data?.message || "Không thể hủy đặt phòng", "error");
        }
      }
    );
  }, [fetchBookings, showToast, triggerCustomConfirm]);

  // Define Columns
  const columns = useMemo(
    () => [
      {
        header: 'Khách hàng',
        accessorKey: 'guestName',
        cell: ({ row }) => {
          const bk = row.original;
          return (
            <div className="space-y-1">
              <span className="inline-block px-2 py-0.5 text-[7px] font-black tracking-widest text-slate-500 bg-slate-100 border border-slate-200 uppercase mb-0.5">
                {bk.bookingType && bk.bookingType.toLowerCase() === 'group' ? 'ĐOÀN (GROUP)' : 'ĐƠN LÊ'}
              </span>
              <h5 className="text-xs font-black text-slate-800 uppercase tracking-wider m-0">
                {bk.guestName}
              </h5>
              <p className="text-[9px] text-slate-600 font-bold truncate max-w-[150px]">
                {bk.email}
              </p>
            </div>
          );
        }
      },
      {
        header: 'Mã đơn',
        accessorKey: 'bookingReference',
        cell: ({ getValue }) => (
          <span className="font-mono text-slate-600 font-bold text-xs uppercase">
            {getValue()}
          </span>
        )
      },
      {
        header: 'Loại phòng',
        accessorKey: 'roomType',
        cell: ({ getValue }) => (
          <span className="text-slate-700 font-semibold text-xs">
            {getValue() || 'Standard'}
          </span>
        )
      },
      {
        header: 'Ngày lưu trú',
        cell: ({ row }) => {
          const bk = row.original;
          return (
            <div className="text-[10px] text-slate-600 font-bold uppercase tracking-wider">
              <div>{bk.checkInDate} đến {bk.checkOutDate}</div>
              <div className="text-slate-500 text-[9px] mt-0.5">({bk.nights} đêm)</div>
            </div>
          );
        }
      },
      {
        header: 'Tổng chi phí',
        accessorKey: 'totalAmount',
        cell: ({ getValue }) => (
          <span className="text-xs font-black text-slate-800">
            {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(getValue())}
          </span>
        )
      },
      {
        header: 'Trạng thái',
        accessorKey: 'status',
        cell: ({ getValue }) => {
          const status = getValue() || '';
          const norm = status.toLowerCase().replace('-', ' ').trim();

          let label = status.toUpperCase();
          let badgeClass = 'bg-slate-100 text-slate-600 border border-slate-200';

          if (norm === 'confirmed') {
            label = 'ĐÃ XÁC NHẬN';
            badgeClass = 'bg-green-50 text-green-700 border border-green-200';
          } else if (norm === 'checked in' || norm === 'checkedin') {
            label = 'ĐÃ NHẬN PHÒNG';
            badgeClass = 'bg-blue-50 text-blue-700 border border-blue-200';
          } else if (norm === 'checked out' || norm === 'checkedout') {
            label = 'ĐÃ TRẢ PHÒNG';
            badgeClass = 'bg-slate-100 text-slate-600 border border-slate-200';
          } else if (norm === 'cancelled') {
            label = 'ĐÃ HỦY';
            badgeClass = 'bg-rose-50 text-rose-700 border border-rose-200';
          } else if (norm === 'pending') {
            label = 'CHỜ XỬ LÝ';
            badgeClass = 'bg-amber-50 text-amber-700 border border-amber-250/60';
          } else if (norm === 'paid') {
            label = 'ĐÃ THANH TOÁN';
            badgeClass = 'bg-emerald-50 text-emerald-700 border border-emerald-200';
          } else if (norm === 'partially paid' || norm === 'partiallypaid') {
            label = 'THANH TOÁN 1 PHẦN';
            badgeClass = 'bg-amber-50 text-amber-700 border border-amber-200';
          }

          return (
            <span className={`whitespace-nowrap inline-block px-2.5 py-1 text-[9px] font-black uppercase tracking-wider rounded-sm ${badgeClass}`}>
              {label}
            </span>
          );
        }
      },
      {
        header: 'Thao tác',
        id: 'actions',
        cell: ({ row }) => {
          const bk = row.original;
          const normalizedStatus = String(bk.status || '').toLowerCase().replace('-', ' ').trim();
          const canCheckIn = ['confirmed', 'paid', 'partially paid'].includes(normalizedStatus);
          const isCheckedIn = normalizedStatus === 'checked in' || normalizedStatus === 'checked-in';
          
          const normalizedMethod = String(bk.checkInMethod || bk.checkinmethod || '').toLowerCase();
          const usesFaceId = normalizedMethod === 'face recognition' || normalizedMethod === 'faceid' || normalizedMethod === 'face id';
          const usesQrCode = normalizedMethod === 'qr code' || normalizedMethod === 'qr';

          return (
            <div className="flex flex-wrap items-center gap-1.5 justify-start">
              <button
                onClick={() => setSelectedBooking(bk)}
                className="bg-slate-100 border border-slate-200 text-slate-700 text-[8.5px] font-black uppercase tracking-widest px-3 py-1.5 cursor-pointer flex items-center gap-1 hover:bg-slate-200 hover:text-slate-900 rounded-sm transition-all"
              >
                Chi tiết
              </button>

              {/* Nút check-in thủ công tại quầy */}
              {canCheckIn && (
                <button
                  onClick={async () => {
                    await handleDirectCheckInOut(bk, 'Checked In');
                    fetchBookings();
                  }}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white text-[8.5px] font-black uppercase tracking-widest px-3 py-1.5 cursor-pointer flex items-center gap-1.5 rounded-sm shadow-sm transition-all border-none"
                >
                  <span className="material-symbols-outlined text-[10px]">how_to_reg</span>
                  Check-in tại quầy
                </button>
              )}

              {/* Nút FaceID Check-in */}
              {canCheckIn && usesFaceId && (
                <button
                  onClick={() => onFaceCheckInSelect(bk.id)}
                  disabled={!isManager}
                  title={!isManager ? "Yêu cầu tài khoản Quản lý để thực hiện FaceID check-in" : "Check-in bằng nhận diện khuôn mặt"}
                  className="bg-primary hover:brightness-110 disabled:bg-slate-100 disabled:text-slate-400 disabled:cursor-not-allowed text-white text-[8.5px] font-black uppercase tracking-widest px-3 py-1.5 cursor-pointer flex items-center gap-1.5 rounded-sm shadow-sm transition-all border-none"
                >
                  <span className="material-symbols-outlined text-[10px]">face</span>
                  {isManager ? 'FaceID Check-in' : 'Cần Manager'}
                </button>
              )}

              {/* Nút Quét QR check-in */}
              {canCheckIn && usesQrCode && (
                <button
                  onClick={() => startScanner(bk, 'qr')}
                  className="bg-blue-600 hover:bg-blue-750 text-white text-[8.5px] font-black uppercase tracking-widest px-3 py-1.5 cursor-pointer flex items-center gap-1.5 rounded-sm shadow-sm transition-all border-none"
                >
                  <span className="material-symbols-outlined text-[10px]">qr_code_scanner</span>
                  Quét QR
                </button>
              )}

              {isCheckedIn && (
                <button
                  onClick={async () => {
                    await handleDirectCheckInOut(bk, 'Checked Out');
                    fetchBookings();
                  }}
                  className="bg-indigo-600 hover:bg-indigo-750 text-white text-[8.5px] font-black uppercase tracking-widest px-3 py-1.5 cursor-pointer rounded-sm shadow-sm transition-all border-none"
                >
                  Trả phòng (Check-out)
                </button>
              )}
            </div>
          );
        }
      }
    ],
    [setSelectedBooking, handleViewInvoice, handleDownloadPdf, handleDirectCheckInOut, startScanner, fetchBookings, isManager, setActiveTab, handleCancelClick, onRoomChangeSelect]
  );

  const table = useReactTable({
    data,
    columns,
    pageCount,
    state: {
      pagination,
    },
    onPaginationChange: setPagination,
    manualPagination: true,
    getCoreRowModel: getCoreRowModel(),
  });

  return (
    <div className="space-y-6 text-left">
      {/* Filtering UI */}
      <div className="bg-white border border-slate-200/85 p-5 shadow-sm flex flex-wrap gap-4 items-center justify-between">
        <div className="flex flex-wrap gap-4 items-center flex-1">
          {/* Keyword Search */}
          <div className="relative min-w-[220px] flex-1 max-w-xs">
            <input
              type="text"
              placeholder="Tìm theo tên, email, mã BK..."
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setPagination(prev => ({ ...prev, pageIndex: 0 }));
              }}
              className="w-full bg-transparent border-b border-slate-200 py-2 pr-8 font-bold text-xs outline-none text-slate-800 focus:border-primary placeholder:text-slate-450"
            />
            <span className="absolute right-0 top-1/2 -translate-y-1/2 material-symbols-outlined text-slate-400 text-sm">
              search
            </span>
          </div>

          {/* Filter Status */}
          <div className="flex items-center gap-2">
            <span className="text-[9px] font-black uppercase tracking-widest text-slate-500">Trạng thái:</span>
            <select
              value={status}
              onChange={(e) => {
                setStatus(e.target.value);
                setPagination(prev => ({ ...prev, pageIndex: 0 }));
              }}
              className="bg-transparent border-b border-slate-200 py-1 font-bold text-xs outline-none text-slate-800 focus:border-primary [&>option]:bg-white [&>option]:text-slate-800"
            >
              <option value="">Tất cả</option>
              <option value="Pending">Chờ xử lý</option>
              <option value="Confirmed">Đã xác nhận</option>
              <option value="Paid">Đã thanh toán</option>
              <option value="Partially Paid">Thanh toán 1 phần</option>
              <option value="Checked-in">Đã nhận phòng</option>
              <option value="Checked-out">Đã trả phòng</option>
              <option value="Cancelled">Đã hủy</option>
            </select>
          </div>

          {/* Filter Booking Type */}
          <div className="flex items-center gap-2">
            <span className="text-[9px] font-black uppercase tracking-widest text-slate-500">Loại đơn:</span>
            <select
              value={bookingType}
              onChange={(e) => {
                setBookingType(e.target.value);
                setPagination(prev => ({ ...prev, pageIndex: 0 }));
              }}
              className="bg-transparent border-b border-slate-200 py-1 font-bold text-xs outline-none text-slate-800 focus:border-primary [&>option]:bg-white [&>option]:text-slate-800"
            >
              <option value="">Tất cả</option>
              <option value="Single">Đơn lẻ</option>
              <option value="Group">Đoàn (Group)</option>
            </select>
          </div>
        </div>

        <button
          onClick={() => {
            setKeyword('');
            setStatus('');
            setBookingType('');
            setPagination({ pageIndex: 0, pageSize: 10 });
          }}
          className="text-[9px] font-black uppercase tracking-widest text-slate-600 hover:text-primary border border-slate-200 hover:border-primary/30 px-3 py-2 transition-all bg-transparent cursor-pointer"
        >
          Xóa bộ lọc
        </button>
      </div>

      {/* TanStack Table UI */}
      <div className="bg-white border border-slate-200/85 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-left text-xs text-slate-600">
            <thead>
              {table.getHeaderGroups().map(headerGroup => (
                <tr key={headerGroup.id} className="border-b border-slate-100 text-[9px] font-black uppercase tracking-wider text-slate-500 bg-slate-50/50">
                  {headerGroup.headers.map(header => (
                    <th key={header.id} className="p-4 font-black">
                      {header.isPlaceholder
                        ? null
                        : flexRender(header.column.columnDef.header, header.getContext())}
                    </th>
                  ))}
                </tr>
              ))}
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={columns.length} className="p-12 text-center text-slate-500 font-bold uppercase tracking-wider">
                    <span className="inline-block w-5 h-5 border-2 border-primary border-t-transparent rounded-full animate-spin mr-2 align-middle"></span>
                    Đang tải dữ liệu đặt phòng...
                  </td>
                </tr>
              ) : data.length === 0 ? (
                <tr>
                  <td colSpan={columns.length} className="p-12 text-center text-slate-500 font-bold uppercase tracking-wider">
                    Không tìm thấy dữ liệu đặt phòng nào.
                  </td>
                </tr>
              ) : (
                table.getRowModel().rows.map(row => (
                  <tr key={row.id} className="hover:bg-slate-50/50 transition-colors">
                    {row.getVisibleCells().map(cell => (
                      <td key={cell.id} className="p-4 align-middle">
                        {flexRender(cell.column.columnDef.cell, cell.getContext())}
                      </td>
                    ))}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Server-side Pagination Controls */}
        {data.length > 0 && (
          <div className="p-4 border-t border-slate-100 bg-slate-50/50 flex flex-wrap gap-4 items-center justify-between text-[10px] text-slate-500 font-bold uppercase tracking-widest">
            <div className="flex items-center gap-4">
              <span className="text-slate-500 font-semibold">
                Trang <strong className="text-slate-800">{pagination.pageIndex + 1}</strong> trên <strong className="text-slate-800">{table.getPageCount()}</strong>
              </span>
              <div className="flex items-center gap-1">
                <span className="text-[9px] uppercase tracking-wider text-slate-400">Hiển thị:</span>
                <select
                  value={pagination.pageSize}
                  onChange={e => {
                    setPagination(prev => ({ ...prev, pageSize: Number(e.target.value), pageIndex: 0 }));
                  }}
                  className="bg-white border border-slate-200 text-slate-850 py-1 px-2 font-bold text-[10px] outline-none rounded-sm"
                >
                  {[5, 10, 20, 50].map(pageSize => (
                    <option key={pageSize} value={pageSize}>
                      {pageSize} dòng
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => table.previousPage()}
                disabled={!table.getCanPreviousPage() || loading}
                className="bg-slate-50 border border-slate-200 text-slate-600 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-100 hover:text-slate-800 px-3 py-1.5 transition-all text-[9px] font-black uppercase tracking-widest cursor-pointer"
              >
                Trước
              </button>
              <button
                onClick={() => table.nextPage()}
                disabled={!table.getCanNextPage() || loading}
                className="bg-slate-50 border border-slate-200 text-slate-600 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-100 hover:text-slate-800 px-3 py-1.5 transition-all text-[9px] font-black uppercase tracking-widest cursor-pointer"
              >
                Sau
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default BookingsTable;
