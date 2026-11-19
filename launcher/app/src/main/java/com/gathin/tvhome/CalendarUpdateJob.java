package com.gathin.tvhome;

import android.app.job.*;

/** Inexact background wake-up; repository decides which source checks are due. */
public final class CalendarUpdateJob extends JobService {
    static final int ID=0x484243;
    @Override public boolean onStartJob(JobParameters parameters){
        CalendarData.get(this).refresh(false,()->jobFinished(parameters,false));return true;
    }
    @Override public boolean onStopJob(JobParameters parameters){CalendarData.get(this).cancel();return true;}
}
