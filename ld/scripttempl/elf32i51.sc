cat <<EOF
OUTPUT_FORMAT("${OUTPUT_FORMAT}","${OUTPUT_FORMAT}","${OUTPUT_FORMAT}")
OUTPUT_ARCH(${ARCH})

ENTRY(_reset_handler)

PROVIDE(___sdcc_external_startup = 0x0000);

PHDRS
{
  phdr_code PT_LOAD FLAGS(0x5); /* Read + Execute */
  phdr_data PT_LOAD FLAGS(0x6); /* Read + Write */
}

MEMORY
{
  FLASH   (rx)   : ORIGIN = 0, LENGTH = DEFINED(__FLASH_SIZE__) ? __FLASH_SIZE__ : 8K
  IDATA   (rw!x) : ORIGIN = 0, LENGTH = DEFINED(__IDATA_SIZE__) ? __IDATA_SIZE__ : 128
  XDATA   (rw!x) : ORIGIN = 0, LENGTH = DEFINED(__XDATA_SIZE__) ? __XDATA_SIZE__ : 0
  EEPROM  (rw!x) : ORIGIN = 0, LENGTH = DEFINED(__EPROM_SIZE__) ? __EPROM_SIZE__ : 0
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
    ${RELOCATING+ PROVIDE (__xinit_start = .) ; }
    KEEP(*(.text.xinit))
    ${RELOCATING+ PROVIDE (__xinit_end = .) ; }
    *(.text.*)
    *(.fini)
    ${RELOCATING+ _etext = . ; }
  } ${RELOCATING+ > FLASH} :phdr_code

  __xinit_size = __xinit_end - __xinit_start ;

  /*
    - [.reg        ] 00-1F   - 32 bytes to hold up to 4 banks of the registers R0 to R7,
    - [.bitdata ...] 20-2F   - 16 bytes to hold 128 bit variables and,
    - [.data       ] 30-7F   - 80 bytes for general purpose use.
  */

  .bdata ${RELOCATING+ 0x20} (NOLOAD):
  {
    *(.bitdata*)
    *(.bdata*)
  } ${RELOCATING+ > IDATA}

  .bbss	${RELOCATING+ (SIZEOF(.bdata) + ADDR(.bdata))} (NOLOAD):
  {
    ${RELOCATING+ PROVIDE (__bbss_start = .) ; }
    *(.bitbss*)
    *(.bbss*)
    ${RELOCATING+ PROVIDE (__bbss_end = .) ; }
  } ${RELOCATING+ > IDATA}

  ASSERT (__bbss_end <= 0x2F, "bit data region overflowed.")

  .data	${RELOCATING+ (SIZEOF(.bbss) + ADDR(.bbss))} (NOLOAD):
  {
    *(.data*)
    *(.gnu.linkonce.d*)
  } ${RELOCATING+ > IDATA}

  .bss ${RELOCATING+ (SIZEOF(.data) + ADDR(.data))} (NOLOAD):
  {
    *(.bss*)
    *(COMMON)
  } ${RELOCATING+ > IDATA}

  .idata ${RELOCATING+ (SIZEOF(.bss) + ADDR(.bss))} (NOLOAD):
  {
    *(.idata*)
  } ${RELOCATING+ > IDATA}

  .ibss ${RELOCATING+ (SIZEOF(.idata) + ADDR(.idata))} (NOLOAD):
  {
    *(.ibss*)
    ${RELOCATING+ PROVIDE (__start__stack = .) ; }
    *(.stack)
  } ${RELOCATING+ > IDATA}

  .xdata ${RELOCATING+ 0} (NOLOAD):
  {
    ${RELOCATING+ PROVIDE (__xdata_start = .) ; }
    *(.pdata*)
    *(.xdata*)
    ${RELOCATING+ PROVIDE (__xdata_end = .) ; }
  } ${RELOCATING+ > XDATA}

  __xdata_size = __xdata_end - __xdata_start ;

  .xbss ${RELOCATING+ SIZEOF(.xdata)} (NOLOAD):
  {
    *(.xbss*)
  } ${RELOCATING+ > XDATA}

  .eeprom ${RELOCATING+ 0} (NOLOAD):
  {
    *(.eeprom*)
  } ${RELOCATING+ > EEPROM}

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

