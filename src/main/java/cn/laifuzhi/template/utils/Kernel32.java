package cn.laifuzhi.template.utils;

import com.sun.jna.*;
import com.sun.jna.win32.StdCallLibrary;

/**
 * elasticsearch的JNAKernel32Library使用的jna调用windows系统库
 */
// kernel32.dll uses the __stdcall calling convention (check the function
// declaration for "WINAPI" or "PASCAL"), so extend StdCallLibrary
// Most C libraries will just extend com.sun.jna.Library,
public interface Kernel32 extends StdCallLibrary {
    // Method declarations, constant and structure definitions go here
    Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
    // Optional: wraps every call to the native library in a
    // synchronized block, limiting native calls to one at a time
    Kernel32 SYNC_INSTANCE = (Kernel32) Native.synchronizedLibrary(INSTANCE);

    public static class SizeT extends IntegerType {

        // JNA requires this no-arg constructor to be public,
        // otherwise it fails to register kernel32 library
        public SizeT() {
            this(0);
        }

        SizeT(long value) {
            super(Native.SIZE_T_SIZE, value);
        }

    }

    /**
     * Locks the specified region of the process's virtual address space into physical
     * memory, ensuring that subsequent access to the region will not incur a page fault.
     *
     * https://msdn.microsoft.com/en-us/library/windows/desktop/aa366895%28v=vs.85%29.aspx
     *
     * @param address A pointer to the base address of the region of pages to be locked.
     * @param size The size of the region to be locked, in bytes.
     * @return true if the function succeeds
     */
    boolean VirtualLock(Pointer address, SizeT size);
}
