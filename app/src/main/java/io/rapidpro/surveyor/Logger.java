/*
 * RapidPro Android Channel - Relay SMS messages where MNO connections aren't practical.
 * Copyright (C) 2014 Nyaruka, UNICEF
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.rapidpro.surveyor;

import android.util.Log;

public class Logger {

    public static final String TAG = "Surveyor";

    public void d(String message) {
        Log.d(TAG, message);
    }

    public void e(String message, Throwable t) {
        Log.e(TAG, message, t);
        t.printStackTrace();
    }

    public void v(String message) {
        Log.v(TAG, message);
    }


    public void w(String message) {
        Log.w(TAG, message);
    }

}
