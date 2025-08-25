cat <<EOF
OUTPUT_FORMAT("${OUTPUT_FORMAT}","${OUTPUT_FORMAT}","${OUTPUT_FORMAT}")
OUTPUT_ARCH(${ARCH})

ENTRY(_reset_handler)

/* Provide default sdcc weak symbols */
PROVIDE(___sdcc_external_startup = 0x0000);

/* Provide default size. User can override it by using --defsym=xxx option */
PROVIDE(__FLASH_SIZE__ = 8192);
PROVIDE(__IDATA_SIZE__ = 128);
PROVIDE(__XDATA_SIZE__ = 0);

PHDRS
{
  phdr_code PT_LOAD FLAGS(0x5); /* Read + Execute */
  phdr_data PT_LOAD FLAGS(0x6); /* Read + Write */
}

MEMORY
{
  FLASH   (rx)   : ORIGIN = 0, LENGTH = __FLASH_SIZE__
  BITS    (rw!x) : ORIGIN = 0, LENGTH = 128
  DATA    (rw!x) : ORIGIN = 0, LENGTH = 128
  IDATA   (rw!x) : ORIGIN = 0, LENGTH = __IDATA_SIZE__
  XDATA   (rw!x) : ORIGIN = 0, LENGTH = __XDATA_SIZE__
}

/* end_addr -1 make sure the address always <= 65535 */
__idata_seg_end = (LENGTH(IDATA) > 0) ? (LENGTH(IDATA) - 1) : 0;
__xdata_seg_end = (LENGTH(XDATA) > 0) ? (LENGTH(XDATA) - 1) : 0;

SECTIONS
{
  /* Read-only sections, merged into text segment: */
  ${TEXT_DYNAMIC+${DYNAMIC}}
  .hash        ${RELOCATING-0} : { *(.hash)		}
  .dynsym      ${RELOCATING-0} : { *(.dynsym)		}
  .dynstr      ${RELOCATING-0} : { *(.dynstr)		}
  .gnu.version ${RELOCATING-0} : { *(.gnu.version)	}
  .gnu.version_d ${RELOCATING-0} : { *(.gnu.version_d)	}
  .gnu.version_r ${RELOCATING-0} : { *(.gnu.version_r)	}

  .rel.init    ${RELOCATING-0} : { *(.rel.init)	}
  .rela.init   ${RELOCATING-0} : { *(.rela.init)	}
  .rel.text    ${RELOCATING-0} :
    {
      *(.rel.text)
      ${RELOCATING+*(.rel.text.*)}
      ${RELOCATING+*(.rel.gnu.linkonce.t*)}
    }
  .rela.text   ${RELOCATING-0} :
    {
      *(.rela.text)
      ${RELOCATING+*(.rela.text.*)}
      ${RELOCATING+*(.rela.gnu.linkonce.t*)}
    }
  .rel.fini    ${RELOCATING-0} : { *(.rel.fini)	}
  .rela.fini   ${RELOCATING-0} : { *(.rela.fini)	}
  .rel.rodata  ${RELOCATING-0} :
    {
      *(.rel.rodata)
      ${RELOCATING+*(.rel.rodata.*)}
      ${RELOCATING+*(.rel.gnu.linkonce.r*)}
    }
  .rela.rodata ${RELOCATING-0} :
    {
      *(.rela.rodata)
      ${RELOCATING+*(.rela.rodata.*)}
      ${RELOCATING+*(.rela.gnu.linkonce.r*)}
    }
  .rel.data    ${RELOCATING-0} :
    {
      *(.rel.data)
      ${RELOCATING+*(.rel.data.*)}
      ${RELOCATING+*(.rel.gnu.linkonce.d*)}
    }
  .rela.data   ${RELOCATING-0} :
    {
      *(.rela.data)
      ${RELOCATING+*(.rela.data.*)}
      ${RELOCATING+*(.rela.gnu.linkonce.d*)}
    }
  .rel.ctors   ${RELOCATING-0} : { *(.rel.ctors)	}
  .rela.ctors  ${RELOCATING-0} : { *(.rela.ctors)	}
  .rel.dtors   ${RELOCATING-0} : { *(.rel.dtors)	}
  .rela.dtors  ${RELOCATING-0} : { *(.rela.dtors)	}
  .rel.got     ${RELOCATING-0} : { *(.rel.got)		}
  .rela.got    ${RELOCATING-0} : { *(.rela.got)		}
  .rel.bss     ${RELOCATING-0} : { *(.rel.bss)		}
  .rela.bss    ${RELOCATING-0} : { *(.rela.bss)		}
  .rel.plt     ${RELOCATING-0} : { *(.rel.plt)		}
  .rela.plt    ${RELOCATING-0} : { *(.rela.plt)		}

  /* Internal text space or external memory */

  .text :
  {
    _reset_handler = . ;
    KEEP(*(.interrupt_vectors))
    KEEP(*(.init.0))
    KEEP(*(.init.1))
    KEEP(*(.init.2))
    KEEP(*(.init.3))
    *(.progmem.gcc*)
    *(.progmem*)
    *(.text)
    ${RELOCATING+ PROVIDE(__xinit_start = .); }
    KEEP(*(.text.xinit))
    ${RELOCATING+ PROVIDE(__xinit_end = .); }
    *(.text.*)
    *(.fini)
    ${RELOCATING+ _etext = . ; }
  } ${RELOCATING+ > FLASH} :phdr_code

  __xinit_size = __xinit_end - __xinit_start ;

  /*
    - [.reg  ] 00-1F   - 32 bytes to hold up to 4 banks of the registers R0 to R7,
    - [.bit  ] 20-2F   - 16 bytes to hold 128 bit variables and,
    - [.data ] 30-7F   - 80 bytes for general purpose use.
  */

  .bit ${RELOCATING+ 0} (NOLOAD):
  {
    . += 8 ; /* Reserve 1 byte for sdcc built-in 'bits' */
    *(.bitdata*)
    *(.bitbss*)
  } ${RELOCATING+ > BITS}

  .data	${RELOCATING+ 0} (NOLOAD):
  {
    . += 0x20 ; /* Reserve space for the work registers R0 to R7 */
    . += (SIZEOF(.bit) + 7) / 8 ; /* Reserve space for the '.bit' section */
    *(.data*)
    *(.gnu.linkonce.d*)
    *(.bss*)
    *(COMMON)
  } ${RELOCATING+ > DATA}

  .idata ${RELOCATING+ 0} (NOLOAD):
  {
    . += SIZEOF(.data) ;
    *(.idata*)
    *(.ibss*)
    ${RELOCATING+ __start__stack = . ; }
  } ${RELOCATING+ > IDATA}

  ASSERT ((LENGTH(IDATA) - SIZEOF(.idata)) >= 20, "error: No space left for the stack, at least 20 bytes.")

  .xdata ${RELOCATING+ 0} (NOLOAD):
  {
    KEEP(*(.pdata*))
    ${RELOCATING+ PROVIDE(__xdata_start = .); }
    KEEP(*(.xdata*))
    ${RELOCATING+ PROVIDE(__xdata_end = .); }
    *(.xbss*)
  } ${RELOCATING+ > XDATA}

  /* indicate the copy/clear address is overflowed (address > 0xFFFF ?)
      0: not
      1: clear end address is overflowed
      2: clear xdata/copy end address is overflowed
  */
  __xdata_ov_flag = (__xdata_end - __xdata_start > 0xFFFF) ? 2 : (SIZEOF(.xdata) > 0xFFFF) ;
  /* determine if we need copy xdata from flash */
  __xdata_has_copy = (__xdata_end - __xdata_start) > 0 ;
  /* xdata copy end address (contain end address) */
  __xdata_copy_end = (__xdata_end > 0xFFFF) ? 0xFFFF : __xdata_end ;
  /* xdata clear-zero end address (contain end address) */
  __xdata_clear_end = SIZEOF(.xdata) > 0xFFFF ? 0xFFFF : SIZEOF(.xdata) ;

  /* dummy section just used to generate warnings */
  .abs_dummy ${RELOCATING+ 0} (NOLOAD):
  {
    KEEP(*(.abs*))
  }
  ASSERT (SIZEOF(.abs_dummy) == 0, "error: Do not define ABS variables in your code. Please use linker script instead.")

  /* Stabs debugging sections.  */
  .stab 0 : { *(.stab) }
  .stabstr 0 : { *(.stabstr) }
  .stab.excl 0 : { *(.stab.excl) }
  .stab.exclstr 0 : { *(.stab.exclstr) }
  .stab.index 0 : { *(.stab.index) }
  .stab.indexstr 0 : { *(.stab.indexstr) }
  .comment 0 : { *(.comment) }
 
  /* DWARF debug sections.
     Symbols in the DWARF debugging sections are relative to the beginning
     of the section so we begin them at 0.  */

  /* DWARF 1 */
  .debug          0 : { *(.debug) }
  .line           0 : { *(.line) }

  /* GNU DWARF 1 extensions */
  .debug_srcinfo  0 : { *(.debug_srcinfo) }
  .debug_sfnames  0 : { *(.debug_sfnames) }

  /* DWARF 1.1 and DWARF 2 */
  .debug_aranges  0 : { *(.debug_aranges) }
  .debug_pubnames 0 : { *(.debug_pubnames) }

  /* DWARF 2 */
  .debug_info     0 : { *(.debug_info) *(.gnu.linkonce.wi.*) }
  .debug_abbrev   0 : { *(.debug_abbrev) }
  .debug_line     0 : { *(.debug_line) }
  .debug_frame    0 : { *(.debug_frame) }
  .debug_str      0 : { *(.debug_str) }
  .debug_loc      0 : { *(.debug_loc) }
  .debug_macinfo  0 : { *(.debug_macinfo) }
}
EOF

