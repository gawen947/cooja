/* Copyright (c) 2016, David Hauweele <david@hauweele.net>
   All rights reserved.

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are met:

    1. Redistributions of source code must retain the above copyright notice, this
       list of conditions and the following disclaimer.
    2. Redistributions in binary form must reproduce the above copyright notice,
       this list of conditions and the following disclaimer in the documentation
       and/or other materials provided with the distribution.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
   ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
   ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
   (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
   ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package be.ac.umons.cooja.monitor.device;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.contikios.cooja.mspmote.MspMote;
import org.contikios.cooja.mspmote.MspMoteType;
import org.contikios.cooja.Simulation;

import be.ac.umons.cooja.monitor.mon.MonTimestamp;
import be.ac.umons.cooja.monitor.mon.backend.MonBackend;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.Memory.AccessMode;
import se.sics.mspsim.core.Memory.AccessType;
import se.sics.mspsim.core.MemoryMonitor;
import se.sics.mspsim.util.MapTable;

public class MemMon implements MemoryMonitor, MonDevice {
  private static Logger logger = Logger.getLogger(MemMon.class);

  public final int monctx; /* context */
  public final int monent; /* entity */
  public final int monsti; /* state/info */
  public final int monctl; /* len/control */

  /* CTL register:
     9:   mode (0: event / 1: info)
     8:   record event/info
     7-0: info len
   */

  private int ctx;
  private int ent;
  private int sti;
  private int len;

  private final MonBackend backend;
  private final MSP430     cpu;
  private final Simulation simulation;
  private final short      nodeID;

  public MemMon(MspMote mspMote, MonBackend backend, Simulation simulation, short nodeID) {
    this.backend    = backend;
    this.simulation = simulation;
    this.nodeID     = nodeID;

    logger.info("Starting MemMon device");

    MapTable symTbl;

    try {
      symTbl = ((MspMoteType)mspMote.getType()).getELF().getMap();
    } catch ( IOException e ) {
      logger.error("Cannot read ELF");

      cpu    = null;
      monctx = -1;
      monent = -1;
      monsti = -1;
      monctl = -1;

      return;
    }

    cpu = mspMote.getCPU();

    /* Find monitor symbols */
    monctx = symTbl.getFunctionAddress("memmon_reg_ctx");
    monent = symTbl.getFunctionAddress("memmon_reg_ent");
    monsti = symTbl.getFunctionAddress("memmon_reg_sti");
    monctl = symTbl.getFunctionAddress("memmon_reg_ctl");

    if(monctx < 0 || monent < 0 ||
       monsti < 0 || monctl < 0) {
      logger.warn("Cannot find monitor symbols");
      logger.warn("MemMon device disabled"); /* FIXME: Add node identifier. */
      return; /* don't add watch point */
    }

    /* Add watchpoints */
    cpu.addWatchPoint(monctx, this);
    cpu.addWatchPoint(monent, this);
    cpu.addWatchPoint(monsti, this);
    cpu.addWatchPoint(monctl, this);
  }

  @Override
  public void notifyWriteAfter(int addr, int data, AccessMode mode) {
    if(addr == monctx)
      ctx = data;
    else if(addr == monent)
      ent = data;
    else if(addr == monsti)
      sti = data;
    else if(addr == monctl) {
      len = data & 0xff;

      if((data & 0x100) != 0) {
        if((data & 0x200) != 0)
          recordInfo();
        else
          recordState();
      }
    }
  }

  private void recordState() {
    /* sti is state */
    backend.state(ctx, ent, sti,
                  new MonTimestamp(cpu.cycles, cpu.getTimeMillis()), simulation.getSimulationTime(), nodeID);
  }

  private void recordInfo() {
    byte[] info = new byte[len];

    /* sti is info ptr */
    for(int i = 0 ; i < len ; i++)
      /* cpu memory has a byte granularity.
         great for us. */
      info[i] = (byte)cpu.memory[sti + i];

    backend.info(ctx, ent, info,
                 new MonTimestamp(cpu.cycles, cpu.getTimeMillis()), simulation.getSimulationTime(), nodeID);
  }

  @Override
  public void notifyReadAfter(int addr, AccessMode mode, AccessType type) {}

  @Override
  public void notifyReadBefore(int addr, AccessMode mode, AccessType type) {}

  @Override
  public void notifyWriteBefore(int addr, int data, AccessMode mode) {}
}
