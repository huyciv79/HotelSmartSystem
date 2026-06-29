import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  useReactTable,
  getCoreRowModel,
  flexRender,
} from '@tanstack/react-table';
import { filterBookings } from '../../services/bookingService';

const BookingsTable = ({
  showToast,
  setSelectedBooking,
  startScanner,
  handleDirectCheckInOut,
  handleViewInvoice,
  handleDownloadPdf,
  isManager,
  setActiveTab,
  onFaceCheckInSelect
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

  // Expose reload listener
  useEffect(() => {
    const handleReload = () => fetchBookings();
    window.addEventListener('reload-bookings', handleReload);
    return () => window.removeEventListener('reload-bookings', handleReload);
  }, [fetchBookings]);

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
              <span className="inline-block px-2 py-0.5 text-[7px] font-black tracking-widest text-slate-400 bg-neutral-900 border border-neutral-800 uppercase mb-0.5">
                {bk.bookingType && bk.bookingType.toLowerCase() === 'group' ? 'ĐOÀN (GROUP)' : 'ĐƠN LẺ'}
              </span>
              <h5 className="text-xs font-black text-white uppercase tracking-wider m-0">
                {bk.guestName}
              </h5>
              <p className="text-[9px] text-slate-500 font-bold truncate max-w-[150px]">
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
          <span className="font-mono text-slate-300 font-bold text-xs uppercase">
            {getValue()}
          </span>
        )
      },
      {
        header: 'Loại phòng',
        accessorKey: 'roomType',
        cell: ({ getValue }) => (
          <span className="text-slate-300 font-semibold text-xs">
            {getValue() || 'Standard'}
          </span>
        )
      },
      {
        header: 'Ngày lưu trú',
        cell: ({ row }) => {
          const bk = row.original;
          return (
            <div className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">
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
          <span className="text-xs font-black text-white">
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
          let badgeClass = 'bg-neutral-900 text-slate-400 border border-neutral-800';

          if (norm === 'confirmed') {
            label = 'ĐÃ XÁC NHẬN';
            badgeClass = 'bg-green-500/10 text-green-400 border border-green-500/20';
          } else if (norm === 'checked in' || norm === 'checkedin') {
            label = 'ĐÃ NHẬN PHÒNG';
            badgeClass = 'bg-blue-500/10 text-blue-400 border border-blue-500/20';
          } else if (norm === 'checked out' || norm === 'checkedout') {
            label = 'ĐÃ TRẢ PHÒNG';
            badgeClass = 'bg-neutral-800 text-slate-400 border border-neutral-700/60';
          } else if (norm === 'cancelled') {
            label = 'ĐÃ HỦY';
            badgeClass = 'bg-rose-500/10 text-rose-400 border border-rose-500/20';
          } else if (norm === 'pending') {
            label = 'CHỜ XỬ LÝ';
            badgeClass = 'bg-yellow-500/10 text-yellow-400 border border-yellow-500/20';
          } else if (norm === 'paid') {
            label = 'ĐÃ THANH TOÁN';
            badgeClass = 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20';
          } else if (norm === 'partially paid' || norm === 'partiallypaid') {
            label = 'THANH TOÁN 1 PHẦN';
            badgeClass = 'bg-amber-500/10 text-amber-400 border border-amber-500/20';
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
          const isConfirmed = bk.status === 'Confirmed';
          const isCheckedIn = bk.status === 'Checked In';
          const usesFaceId = bk.checkInMethod === 'Face Recognition' || bk.checkInMethod === 'FaceID';

          return (
            <div className="flex flex-wrap items-center gap-1.5 justify-start">
              <button
                onClick={() => setSelectedBooking(bk)}
                className="bg-slate-900/50 border border-slate-700 text-slate-300 text-[8.5px] font-black uppercase tracking-widest px-2.5 py-1.5 cursor-pointer flex items-center gap-1 hover:bg-slate-800 transition-colors"
              >
                Chi tiết
              </button>
              <button
                onClick={() => handleViewInvoice(bk.id)}
                className="bg-amber-950/20 border border-amber-900/50 text-amber-400 text-[8.5px] font-black uppercase tracking-widest px-2.5 py-1.5 cursor-pointer flex items-center gap-1 hover:bg-amber-900/30 transition-colors"
              >
                Hóa đơn
              </button>
              <button
                onClick={() => handleDownloadPdf(bk.id, bk.bookingReference)}
                className="bg-emerald-950/20 border border-emerald-900/50 text-emerald-400 text-[8.5px] font-black uppercase tracking-widest px-2.5 py-1.5 cursor-pointer flex items-center gap-1 hover:bg-emerald-900/30 transition-colors"
              >
                Tải PDF
              </button>
              {isConfirmed && (
                <button
                  onClick={() => onFaceCheckInSelect(bk.id)}
                  className="bg-primary hover:brightness-110 text-white text-[8.5px] font-black uppercase tracking-widest px-2.5 py-1.5 border border-primary/20 cursor-pointer flex items-center gap-1 transition-all"
                >
                  FaceID Check-in
                </button>
              )}
              {isCheckedIn && (
                <button
                  onClick={async () => {
                    await handleDirectCheckInOut(bk, 'Checked Out');
                    fetchBookings();
                  }}
                  className="bg-indigo-600 hover:bg-indigo-500 text-white text-[8.5px] font-black uppercase tracking-widest px-3 py-1.5 border border-indigo-500/20 cursor-pointer transition-all"
                >
                  Trả phòng (Check-out)
                </button>
              )}
            </div>
          );
        }
      }
    ],
    [setSelectedBooking, handleViewInvoice, handleDownloadPdf, handleDirectCheckInOut, startScanner, fetchBookings, isManager, setActiveTab]
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
      <div className="bg-[#0f0f12] border border-neutral-900 p-5 shadow-sm flex flex-wrap gap-4 items-center justify-between">
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
              className="w-full bg-transparent border-b border-neutral-800 py-2 pr-8 font-bold text-xs outline-none text-white focus:border-primary placeholder:text-slate-600"
            />
            <span className="absolute right-0 top-1/2 -translate-y-1/2 material-symbols-outlined text-slate-500 text-sm">
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
              className="bg-transparent border-b border-neutral-800 py-1 font-bold text-xs outline-none text-white focus:border-primary [&>option]:bg-neutral-900 [&>option]:text-white"
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
              className="bg-transparent border-b border-neutral-800 py-1 font-bold text-xs outline-none text-white focus:border-primary [&>option]:bg-neutral-900 [&>option]:text-white"
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
          className="text-[9px] font-black uppercase tracking-widest text-slate-400 hover:text-primary border border-neutral-800 hover:border-primary/30 px-3 py-2 transition-all bg-transparent cursor-pointer"
        >
          Xóa bộ lọc
        </button>
      </div>

      {/* TanStack Table UI */}
      <div className="bg-[#0f0f12] border border-neutral-900 shadow-md overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-left text-xs text-slate-400">
            <thead>
              {table.getHeaderGroups().map(headerGroup => (
                <tr key={headerGroup.id} className="border-b border-neutral-900 text-[9px] font-black uppercase tracking-wider text-slate-500 bg-neutral-950/15">
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
            <tbody className="divide-y divide-neutral-900/60">
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
                  <tr key={row.id} className="hover:bg-white/[0.02] transition-colors">
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
          <div className="p-4 border-t border-neutral-900 bg-neutral-950/10 flex flex-wrap gap-4 items-center justify-between text-[10px] text-slate-500 font-bold uppercase tracking-widest">
            <div className="flex items-center gap-4">
              <span className="text-slate-400">
                Trang <strong className="text-white">{pagination.pageIndex + 1}</strong> trên <strong className="text-white">{table.getPageCount()}</strong>
              </span>
              <div className="flex items-center gap-1">
                <span className="text-[9px] uppercase tracking-wider">Hiển thị:</span>
                <select
                  value={pagination.pageSize}
                  onChange={e => {
                    setPagination(prev => ({ ...prev, pageSize: Number(e.target.value), pageIndex: 0 }));
                  }}
                  className="bg-neutral-900 border border-neutral-800 text-white py-1 px-2 font-bold text-[10px] outline-none rounded-sm"
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
                className="bg-neutral-900 border border-neutral-800 text-slate-400 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-neutral-800 hover:text-white px-3 py-1.5 transition-all text-[9px] font-black uppercase tracking-widest cursor-pointer"
              >
                Trước
              </button>
              <button
                onClick={() => table.nextPage()}
                disabled={!table.getCanNextPage() || loading}
                className="bg-neutral-900 border border-neutral-800 text-slate-400 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-neutral-800 hover:text-white px-3 py-1.5 transition-all text-[9px] font-black uppercase tracking-widest cursor-pointer"
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
