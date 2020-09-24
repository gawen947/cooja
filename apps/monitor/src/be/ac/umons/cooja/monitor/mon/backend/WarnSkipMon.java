/* Copyright (c) 2015, David Hauweele <david@hauweele.net>
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

package be.ac.umons.cooja.monitor.mon.backend;

import org.apache.log4j.Logger;

import be.ac.umons.cooja.monitor.mon.MonStats;
import be.ac.umons.cooja.monitor.mon.MonTimestamp;
import be.ac.umons.cooja.monitor.mon.switchable.SwitchableMonBackend;

/**
 * Warns when an event is skipped because no backend has been configured.
 */
public class WarnSkipMon extends SwitchableMon {
  private static Logger logger = Logger.getLogger(WarnSkipMon.class);

  public WarnSkipMon(MonStats stats) {
    super(stats);
  }
  
  @Override
  protected void skipState(int context, int entity, int state, MonTimestamp timestamp, long simTime, short nodeID) {
    logger.info(String.format("(mon) warning: state event skipped at (node: %d cpu: %f ms, %l cycles; sim: %f ms) ",
                                     timestamp.getMillis(), timestamp.getCycles(), simTime / 1000.));
  }
  @Override
  protected void skipInfo(int context, int entity, byte[] info, MonTimestamp timestamp, long simTime, short nodeID) {
    logger.info(String.format("(mon) warning: info event skipped at (node: %d cpu: %f ms, %l cycles; sim: %f ms)",
                                     timestamp.getMillis(), timestamp.getCycles(), simTime / 1000.));
  }
  @Override
  protected void initSkip(SwitchableMonBackend backend) {}
  @Override
  protected void destroySkip(SwitchableMonBackend backend) {}
}
