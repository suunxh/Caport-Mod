package io.github.suunxh.housingteleporthelper.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class WorldJoinNoticeTest {
    @Test public void waitsForPlayerAndAnnouncesOnlyOnce() {
        WorldJoinNotice notice=new WorldJoinNotice(); Object world=new Object();
        assertFalse(notice.shouldShow(null,false)); assertFalse(notice.shouldShow(world,false));
        assertTrue(notice.shouldShow(world,true)); assertFalse(notice.shouldShow(world,true));
        assertFalse(notice.shouldShow(world,false)); assertFalse(notice.shouldShow(world,true));
    }
    @Test public void announcesWorldChangesAndReconnects() {
        WorldJoinNotice notice=new WorldJoinNotice(); Object first=new Object(), second=new Object();
        assertTrue(notice.shouldShow(first,true)); assertTrue(notice.shouldShow(second,true));
        assertFalse(notice.shouldShow(null,false)); assertTrue(notice.shouldShow(second,true));
    }
    @Test public void leavingBeforePlayerReadyDoesNotAnnounceOldWorld() {
        WorldJoinNotice notice=new WorldJoinNotice();
        assertFalse(notice.shouldShow(new Object(),false)); assertFalse(notice.shouldShow(null,true));
        assertTrue(notice.shouldShow(new Object(),true));
    }
}
