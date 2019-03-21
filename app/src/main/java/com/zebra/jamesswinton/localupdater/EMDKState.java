package com.zebra.jamesswinton.localupdater;

public class EMDKState {

  private boolean isRunning = false;
  private ChangeListener listener;

  public boolean isRunning() {
    return isRunning;
  }

  public void setRunning(boolean running) {
    this.isRunning = running;
    if (listener != null) listener.onChange(running);
  }

  public ChangeListener getListener() {
    return listener;
  }

  public void setListener(ChangeListener listener) {
    this.listener = listener;
  }

  public interface ChangeListener {
    void onChange(boolean state);
  }
}
