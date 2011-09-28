/*
 * Copyright (c) 2007-2011 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cascading.flow.stream;

import cascading.CascadingException;
import cascading.flow.FlowProcess;
import cascading.operation.Filter;
import cascading.pipe.Each;
import cascading.pipe.OperatorException;
import cascading.tuple.TupleEntry;

/**
 *
 */
public class FilterEachStage extends EachStage
  {
  private Filter filter;

  public FilterEachStage( FlowProcess flowProcess, Each each )
    {
    super( flowProcess, each );
    }

  @Override
  public void initialize()
    {
    super.initialize();

    filter = each.getFilter();
    }

  @Override
  public void receive( Duct previous, TupleEntry incomingEntry )
    {
    argumentsEntry.setTuple( incomingEntry.selectTuple( argumentsSelector ) );

    try
      {
      if( filter.isRemove( flowProcess, operationCall ) )
        return;

      next.receive( this, incomingEntry );
      }
    catch( CascadingException exception )
      {
      handleException( exception, incomingEntry );
      }
    catch( Throwable throwable )
      {
      handleException( new OperatorException( each, "operator Each failed executing operation", throwable ), incomingEntry );
      }
    }
  }
