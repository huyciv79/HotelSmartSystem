import { useState } from 'react';

const RoomsManager = ({
  roomsList,
  roomsTotalPages,
  roomsCurrentPage,
  roomsFilterStatus,
  setRoomsFilterStatus,
  roomsFilterType,
  setRoomsFilterType,
  roomsSearchQuery,
  setRoomsSearchQuery,
  fetchRealRooms,
  handleOpenEditRoomItem,
  roomTypes
}) => {
  return (
    <div className="space-y-6 animate-scale-in text-left">
      <div className="border-b border-neutral-900 pb-4">
        <h3 className="text-white font-black text-base uppercase tracking-wider m-0">QUẢN LÝ DANH SÁCH PHÒNG</h3>
        <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Cập nhật và theo dõi trạng thái hoạt động của từng phòng vật lý</p>
      </div>

      {/* Search and Filters */}
      <div className="bg-[#0f0f12] border border-neutral-900 p-6 shadow-sm flex flex-wrap gap-4 items-center justify-between">
        <div className="flex flex-wrap gap-4 items-center flex-1">
          <div className="relative min-w-[200px] flex-1 max-w-xs">
            <input
              type="text"
              placeholder="Tìm số phòng..."
              value={roomsSearchQuery}
              onChange={(e) => setRoomsSearchQuery(e.target.value)}
              className="w-full bg-transparent border-b border-neutral-800 py-2 pr-8 font-bold text-xs outline-none text-white focus:border-primary placeholder:text-slate-600"
            />
            <button
              onClick={() => fetchRealRooms(0)}
              className="absolute right-0 top-1/2 -translate-y-1/2 bg-transparent border-none text-slate-500 hover:text-primary cursor-pointer p-1"
            >
              <span className="material-symbols-outlined text-sm">search</span>
            </button>
          </div>

          {/* Filter Status */}
          <div className="flex items-center gap-2">
            <span className="text-[9px] font-black uppercase tracking-widest text-slate-500">Trạng thái:</span>
            <select
              value={roomsFilterStatus}
              onChange={(e) => setRoomsFilterStatus(e.target.value)}
              className="bg-transparent border-b border-neutral-800 py-1 font-bold text-xs outline-none text-white focus:border-primary [&>option]:bg-neutral-900 [&>option]:text-white"
            >
              <option value="">Tất cả</option>
              <option value="Available">Available</option>
              <option value="Occupied">Occupied</option>
              <option value="Cleaning">Cleaning</option>
              <option value="Maintenance">Maintenance</option>
            </select>
          </div>

          {/* Filter Room Type */}
          <div className="flex items-center gap-2">
            <span className="text-[9px] font-black uppercase tracking-widest text-slate-500">Loại phòng:</span>
            <select
              value={roomsFilterType}
              onChange={(e) => setRoomsFilterType(e.target.value)}
              className="bg-transparent border-b border-neutral-800 py-1 font-bold text-xs outline-none text-white focus:border-primary [&>option]:bg-neutral-900 [&>option]:text-white"
            >
              <option value="">Tất cả loại phòng</option>
              {roomTypes.map(type => (
                <option key={type.id} value={type.id}>{type.name}</option>
              ))}
            </select>
          </div>
        </div>

        <button
          onClick={() => {
            setRoomsSearchQuery('');
            setRoomsFilterStatus('');
            setRoomsFilterType('');
            setTimeout(() => fetchRealRooms(0), 0);
          }}
          className="text-[9px] font-black uppercase tracking-widest text-slate-400 hover:text-primary border border-neutral-800 hover:border-primary/30 px-3 py-2 transition-all bg-transparent cursor-pointer"
        >
          Xóa bộ lọc
        </button>
      </div>

      {/* Rooms list table */}
      <div className="bg-[#0f0f12] border border-neutral-900 shadow-md overflow-hidden">
        <table className="w-full border-collapse text-left text-xs text-slate-400">
          <thead>
            <tr className="border-b border-neutral-900 text-[9px] font-black uppercase tracking-wider text-slate-500">
              <th className="p-4">Số phòng</th>
              <th className="p-4">Tầng</th>
              <th className="p-4">Loại phòng</th>
              <th className="p-4">Mật mã phòng</th>
              <th className="p-4">Trạng thái</th>
              <th className="p-4 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-900/60">
            {roomsList.length === 0 ? (
              <tr>
                <td colSpan="6" className="p-8 text-center text-slate-500 font-bold uppercase tracking-wider">
                  Không tìm thấy phòng nào phù hợp.
                </td>
              </tr>
            ) : (
              roomsList.map((room) => (
                <tr key={room.roomId || room.id} className="hover:bg-white/5 transition-colors">
                  <td className="p-4 text-white font-black text-sm">
                    {room.roomnumber || room.roomNumber}
                  </td>
                  <td className="p-4 text-slate-500 font-bold">
                    Tầng {room.floornumber !== undefined ? room.floornumber : room.floorNumber}
                  </td>
                  <td className="p-4 text-slate-300 font-extrabold uppercase">
                    {room.roomtypename || room.roomTypeName || (room.roomType && room.roomType.name) || 'Elysian Suite'}
                  </td>
                  <td className="p-4 text-slate-400 font-mono tracking-widest text-[11px]">
                    {room.adminpasscode || room.adminPasscode || 'N/A'}
                  </td>
                  <td className="p-4">
                    <span className={`px-2.5 py-1 text-[8px] font-black uppercase tracking-widest ${
                      room.status === 'Available'
                        ? 'bg-green-900/30 text-green-400 border border-green-900/50'
                        : room.status === 'Occupied'
                        ? 'bg-blue-900/30 text-blue-400 border border-blue-900/50'
                        : room.status === 'Cleaning'
                        ? 'bg-amber-900/30 text-amber-400 border border-amber-900/50'
                        : 'bg-neutral-800 text-neutral-400 border border-neutral-700/50'
                    }`}>
                      {room.status}
                    </span>
                  </td>
                  <td className="p-4 text-right">
                    <button
                      onClick={() => handleOpenEditRoomItem(room)}
                      className="bg-neutral-900 border border-neutral-800 text-white hover:bg-primary hover:border-primary text-[9px] font-black uppercase tracking-widest px-3 py-1.5 cursor-pointer flex items-center gap-1 ml-auto"
                    >
                      Cập nhật
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Pagination Controls */}
        {roomsTotalPages > 1 && (
          <div className="border-t border-neutral-900 px-6 py-4 flex justify-between items-center text-[10px] font-black uppercase tracking-wider text-slate-500">
            <span>Trang {roomsCurrentPage + 1} / {roomsTotalPages}</span>
            <div className="flex gap-2">
              <button
                onClick={() => fetchRealRooms(roomsCurrentPage - 1)}
                disabled={roomsCurrentPage === 0}
                className="bg-neutral-900 border border-neutral-800 text-white hover:border-primary px-3 py-1.5 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer transition-all"
              >
                Trước
              </button>
              <button
                onClick={() => fetchRealRooms(roomsCurrentPage + 1)}
                disabled={roomsCurrentPage === roomsTotalPages - 1}
                className="bg-neutral-900 border border-neutral-800 text-white hover:border-primary px-3 py-1.5 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer transition-all"
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

export default RoomsManager;
