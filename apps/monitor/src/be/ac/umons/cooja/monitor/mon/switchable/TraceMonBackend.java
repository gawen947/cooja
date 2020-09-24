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

package be.ac.umons.cooja.monitor.mon.switchable;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;

import org.apache.log4j.Logger;

import be.ac.umons.cooja.monitor.mon.MonException;
import be.ac.umons.cooja.monitor.mon.MonTimestamp;
import be.ac.umons.cooja.monitor.mon.multinode.Event;
import be.ac.umons.cooja.monitor.mon.multinode.EventElement;
import be.ac.umons.cooja.monitor.mon.multinode.MonCreateEvent;
import be.ac.umons.cooja.monitor.mon.multinode.MonDataEvent;
import be.ac.umons.cooja.monitor.mon.multinode.MonStateEvent;
import be.ac.umons.cooja.monitor.mon.multinode.NodeScope;
import be.ac.umons.cooja.monitor.mon.multinode.SimulationScope;
import be.ac.umons.cooja.monitor.mon.multinode.TraceFile;

/**
 * Records events into a trace file (see TraceFile).
 */
public class TraceMonBackend extends SwitchableMonBackend {
  private static short  DEFAULT_NODE_ID = 0;    /* Default node identifier used. */
  private static Logger logger = Logger.getLogger(TraceMonBackend.class);

  /* Use an instance of this class to tell SwitchableMon how to create this backend. */
  static public class Creator implements SwitchableMonBackendCreator {
    private final File file;

    public Creator(File file) {
      this.file = file;
    }

    @Override
    public SwitchableMonBackend create(MonTimestamp recordOffset, MonTimestamp infoOffset, MonTimestamp byteOffset, ByteOrder byteOrder) throws MonException {
      return new TraceMonBackend(recordOffset, infoOffset, byteOffset, byteOrder, file);
    }
  }

  private final TraceFile trace;

  public TraceMonBackend(MonTimestamp recordOffset, MonTimestamp infoOffset,
                         MonTimestamp byteOffset, ByteOrder byteOrder,
                         File file) throws MonException {
    super(recordOffset, infoOffset, byteOffset, byteOrder);

    try {
      trace = new TraceFile(file);

      writeEvent(new MonTimestamp(0, 0), 0, (short)0, new MonCreateEvent(recordOffset, infoOffset, byteOffset, byteOrder));
    } catch (IOException e) {
      throw new MonException("cannot open/create '" + file.getAbsolutePath() + "'");
    }

    logger.info("(mon) trace backend created!");
  }

  @Override
  public void recordState(int context, int entity, int state, MonTimestamp timestamp, long simTime, short nodeID) throws MonException {
    try {
      writeEvent(timestamp, simTime, nodeID, new MonStateEvent(context, entity, state));
    } catch (IOException e) {
      throw new MonException("cannot write state event");
    }
  }

  @Override
  public void recordInfo(int context, int entity, byte[] info, MonTimestamp timestamp, long simTime, short nodeID) throws MonException {
    try {
      writeEvent(timestamp, simTime, nodeID, new MonDataEvent(context, entity, info));
    } catch (IOException e) {
      throw new MonException("cannot write data event");
    }
  }

  @Override
  public void destroy() throws MonException {
    try {
      trace.destroy();
      logger.info("(mon) file backend closes!");
    } catch (IOException e) {
      throw new MonException("close error");
    }
  }

  private void writeEvent(MonTimestamp timestamp, long simTime, short nodeID, EventElement eventElement) throws IOException {
    Event event = new Event(eventElement);

    event.addScope(new NodeScope(timestamp, nodeID));
    event.addScope(new SimulationScope(simTime));

    trace.write(event);
  }
}
