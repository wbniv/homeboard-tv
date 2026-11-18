package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class BackUndoGestureTest {
    @Test public void shortBackNavigatesAndOrphanReleaseDoesNothing(){
        BackUndoGesture g=new BackUndoGesture();assertFalse(g.release(false));assertTrue(g.begin(10,"one"));assertFalse(g.due(1009));assertTrue(g.release(false));assertFalse(g.due(2000));
    }
    @Test public void thresholdFiresExactlyOnceAndConsumesRelease(){
        BackUndoGesture g=new BackUndoGesture();g.begin(50,"one");assertEquals(50,g.progress(550));assertFalse(g.finish(1049,"one"));assertTrue(g.finish(1050,"one"));assertFalse(g.finish(9000,"one"));assertFalse(g.release(false));
        assertTrue(g.begin(9100,"older"));assertTrue(g.finish(10100,"older"));assertFalse(g.release(false));
    }
    @Test public void repeatedDownCannotRestartOrFireAnotherUndo(){
        BackUndoGesture g=new BackUndoGesture();g.begin(0,"one");assertFalse(g.begin(700,"newer"));assertTrue(g.finish(1000,"one"));assertFalse(g.begin(3000,"older"));assertFalse(g.finish(4000,"older"));assertFalse(g.release(false));
    }
    @Test public void changedHistoryIsNeverUndoneAndItsReleaseIsConsumed(){
        BackUndoGesture g=new BackUndoGesture();g.begin(0,"one");assertFalse(g.finish(1000,"newer"));assertTrue(g.finished());assertFalse(g.release(false));
    }
    @Test public void cancellationAndFocusLossPreventUndoAndNavigation(){
        BackUndoGesture g=new BackUndoGesture();g.begin(0,"one");assertFalse(g.release(true));assertFalse(g.finish(1000,"one"));g.begin(2000,"one");g.cancel();assertFalse(g.finish(5000,"one"));assertFalse(g.release(false));
    }
    @Test public void emptyHistoryStillConsumesACompletedLongPress(){
        BackUndoGesture g=new BackUndoGesture();g.begin(0,"[]");assertTrue(g.finish(1100,"[]"));assertFalse(g.release(false));assertEquals(0,g.progress(2000));
    }
}
