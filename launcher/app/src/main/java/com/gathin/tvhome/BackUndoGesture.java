package com.gathin.tvhome;

import java.util.Objects;

/** One physical press owns one history snapshot; release after completion never navigates. */
final class BackUndoGesture {
    static final long HOLD_MS=1000;
    private boolean down,finished;
    private long started;
    private String history;
    boolean down(){return down;}
    boolean finished(){return finished;}
    boolean begin(long now,String snapshot){
        if(down)return false;
        down=true;finished=false;started=now;history=snapshot;return true;
    }
    int progress(long now){return down?(int)Math.max(0,Math.min(100,(now-started)*100/HOLD_MS)):0;}
    boolean due(long now){return down&&!finished&&now-started>=HOLD_MS;}
    boolean finish(long now,String snapshot){
        if(!due(now))return false;
        finished=true;return Objects.equals(history,snapshot);
    }
    boolean release(boolean canceled){boolean navigate=down&&!finished&&!canceled;down=false;return navigate;}
    void cancel(){down=false;finished=false;}
}
