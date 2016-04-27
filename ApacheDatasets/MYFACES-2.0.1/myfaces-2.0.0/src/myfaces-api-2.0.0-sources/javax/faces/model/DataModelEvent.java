/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package javax.faces.model;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 *
 * @author Thomas Spiegl (latest modification by $Author: skitching $)
 * @version $Revision: 676298 $ $Date: 2008-07-13 05:31:48 -0500 (Sun, 13 Jul 2008) $
 */
public class DataModelEvent extends java.util.EventObject
{
    private static final long serialVersionUID = 1823115573192262656L;
    // FIELDS
    private int _index;
    private Object _data;


    public DataModelEvent(DataModel _model, int _index, Object _data)
    {
        super(_model);
        this._index = _index;
        this._data = _data;
    }


    // METHODS
    public DataModel getDataModel()
    {
        return (DataModel) getSource();
    }

    public Object getRowData()
    {
        return _data;
    }

    public int getRowIndex()
    {
        return _index;
    }

}
