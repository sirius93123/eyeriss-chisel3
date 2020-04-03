package myutil

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.experimental.loadMemoryFromFile
import config._
import java.nio.file.Paths

class ram_sim(val aw: Int, val dw: Int, val path: String) extends BlackBox(Map("aw" -> aw, "dw" -> dw, "path" -> path))
  with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val we = Input(Bool())
    val addr = Input(UInt(aw.W))
    //    val waddr = Input(UInt(aw.W))
    val din = Input(UInt(dw.W))
    val dout = Output(UInt(dw.W))
  })
  setResource("/ram_sim.v")
  //  setResource("/ram.mem")
}

class BRAM(val memW: Int,
           val path: String = Paths.get("./src/main/resources/ram.mem").toString)(implicit p: Parameters)
  extends Module {
  val io = IO(new BRAMInterface(memW))
  val bram = Module(new ram_sim(p(BRAMKey).addrW, memW, path))
  bram.io.clk := clock
  bram.io.we := io.we
  bram.io.addr := io.addr
  bram.io.din := io.din
  io.dout := bram.io.dout
}

class RAM(val aw: Int = 20, val dw: Int = 280) extends Module {
  val io = IO(new Bundle {
    val we = Input(Bool())
    val addr = Input(UInt(aw.W))
    //    val waddr = Input(UInt(aw.W))
    val din = Input(Vec(dw / 8, SInt(8.W)))
    val dout = Output(Vec(dw / 8, SInt(8.W)))
  })
  val u = Module(new ram_sim(aw, dw, Paths.get("./src/main/resources/ram.mem").toString))
  u.io.clk := clock
  u.io.we := io.we
  u.io.addr := io.addr
  //  u.io.waddr := io.waddr
  u.io.din := io.din.asUInt()
  //  io.dout := u.io.dout
  for (i <- 0 until dw / 8) {
    io.dout(i) := u.io.dout(i * 8 + 7, i * 8).asSInt()
  }

  def read(addr: UInt): Vec[SInt] = {
    io.addr := addr
    io.we := false.B
    io.dout
  }

  def write(addr: UInt, data: SInt): Unit = {
    io.addr := addr
    io.we := true.B
    io.din := data
  }
}

class MemChisel(aw: Int, dw: Int) extends Module {
  val io = IO(new Bundle {
    val raddr = Input(UInt(aw.W))
    val waddr = Input(UInt(aw.W))
    val din = Input(SInt(dw.W))
    val dout = Output(SInt(dw.W))
    val we = Input(Bool())
  })

  val mem = Mem(2048, SInt(dw.W).cloneType)
  loadMemoryFromFile(mem, "ram.mem")

  io.dout := mem.read(io.raddr)
  when(io.we) {
    mem.write(io.waddr, io.din)
  }
}