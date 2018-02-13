// Copyright (C) Yannick Le Roux.
// This file is part of Freeteuse.
//
//   Freeteuse is free software: you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, either version 3 of the License, or
//   (at your option) any later version.
//
//   Freeteuse is distributed in the hope that it will be useful,
//   but WITHOUT ANY WARRANTY; without even the implied warranty of
//   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//   GNU General Public License for more details.
//
//   You should have received a copy of the GNU General Public License
//   along with Freeteuse.  If not, see <http://www.gnu.org/licenses/>.

package bzh.leroux.yannick.freeteuse;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class Home implements FreeboxSniffer.Listener
{
  public interface Listener
  {
    void onFreeboxSelected (Freebox freebox);
    void onFreeboxDetected (Freebox freebox);
  }

  private DnsServiceSniffer mDnsServiceSniffer;
  private Simulator         mSimulator;
  private Context           mContext;
  private SharedPreferences mPreferences;
  private List<Freebox>     mBoxes;
  private Listener          mListener;
  private Painter           mPainter;

  // ---------------------------------------------------
  Home (Context           context,
        Listener          listener,
        SharedPreferences preferences)
  {
    mContext     = context;
    mListener    = listener;
    mPreferences = preferences;
    mPainter     = new Painter ();
    mBoxes       = new ArrayList<> ();
  }

  // ---------------------------------------------------
  private void recoverSavedBoxes ()
  {
    String freeboxPool = mPreferences.getString ("freebox_pool", null);

    if (freeboxPool != null)
    {
      Freebox focus = null;

      try
      {
        JSONArray array = new JSONArray (freeboxPool);

        for (int i = 0; i < array.length (); i++)
        {
          Freebox freebox = new Freebox (array.getJSONObject (i));

          if (freebox.isConsistent ())
          {
            mPainter.useColor (freebox.getColor ());
            mBoxes.add (freebox);

            if (freebox.hasFocus () || (focus == null))
            {
              focus = freebox;
            }
          }
        }
      }
      catch (org.json.JSONException e)
      {
        e.printStackTrace ();
      }

      if (focus != null)
      {
        mListener.onFreeboxSelected (focus);
      }
    }
  }

  // ---------------------------------------------------
  void startDiscovering ()
  {
    recoverSavedBoxes ();

    mDnsServiceSniffer = new DnsServiceSniffer (mContext, this);
    mDnsServiceSniffer.start ();

    mSimulator = new Simulator (mContext, this);
    mSimulator.start ();
  }

  // ---------------------------------------------------
  void stopDiscovering ()
  {
    mDnsServiceSniffer.stop ();
    mSimulator.stop ();

    save ();
  }

  // ---------------------------------------------------
  private void save ()
  {
    JSONArray array = new JSONArray ();

    for (Freebox box : mBoxes)
    {
      JSONObject json = box.getJson ();

      if (json != null)
      {
        array.put (json);
        Log.e ("FreeTeuse", String.valueOf (json));
      }
    }
  }

  // ---------------------------------------------------
  @Override
  public void onFreeboxDetected (Freebox freebox)
  {
    for (Freebox box : mBoxes)
    {
      if (freebox.equals (box))
      {
        return;
      }
    }

    if (freebox.getColor () == null)
    {
      freebox.setColor (mPainter.getColor ());
    }
    mBoxes.add (freebox);
    mListener.onFreeboxDetected (freebox);
  }

  // ---------------------------------------------------
  Freebox GetNextReachable (Freebox of)
  {
    boolean found = false;

    for (Freebox box : mBoxes)
    {
      if (found && box.isReachable ())
      {
        return box;
      }

      if (box == of)
      {
        found = true;
      }
    }

    return null;
  }

  // ---------------------------------------------------
  Freebox GetPreviousReachable (Freebox of)
  {
    Freebox previous = null;

    for (Freebox box : mBoxes)
    {
      if (box == of)
      {
        break;
      }

      if (box.isReachable ())
      {
        previous = box;
      }
    }

    return previous;
  }
}