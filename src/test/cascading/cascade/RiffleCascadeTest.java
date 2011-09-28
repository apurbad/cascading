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

package cascading.cascade;

import java.io.IOException;

import cascading.PlatformTestCase;
import cascading.flow.Flow;
import cascading.flow.hadoop.ProcessFlow;
import cascading.operation.Identity;
import cascading.operation.regex.RegexSplitter;
import cascading.operation.text.FieldJoiner;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tap.SinkMode;
import cascading.tap.Tap;
import cascading.test.PlatformTest;
import cascading.tuple.Fields;
import riffle.process.scheduler.ProcessChain;

import static data.InputData.inputFileIps;

@PlatformTest(platforms = {"local", "hadoop"})
public class RiffleCascadeTest extends PlatformTestCase
  {
  public RiffleCascadeTest()
    {
    super( true );
    }

  private Flow firstFlow( String path )
    {
    Tap source = getPlatform().getTextFile( inputFileIps );

    Pipe pipe = new Pipe( "first" );

    pipe = new Each( pipe, new Fields( "line" ), new Identity( new Fields( "ip" ) ), new Fields( "ip" ) );

    Tap sink = getPlatform().getDelimitedFile( new Fields( "ip" ), getOutputPath( path + "/first" ), SinkMode.REPLACE );

    return getPlatform().getFlowConnector().connect( source, sink, pipe );
    }

  private Flow secondFlow( Tap source, String path )
    {
    Pipe pipe = new Pipe( "second" );

    pipe = new Each( pipe, new RegexSplitter( new Fields( "first", "second", "third", "fourth" ), "\\." ) );

    Tap sink = getPlatform().getDelimitedFile( new Fields( "first", "second", "third", "fourth" ), getOutputPath( path + "/second" ), SinkMode.REPLACE );

    return getPlatform().getFlowConnector().connect( source, sink, pipe );
    }

  private Flow thirdFlow( Tap source, String path )
    {
    Pipe pipe = new Pipe( "third" );

    pipe = new Each( pipe, new FieldJoiner( new Fields( "mangled" ), "-" ) );

    Tap sink = getPlatform().getDelimitedFile( new Fields( "mangled" ), getOutputPath( path + "/third" ), SinkMode.REPLACE );

    return getPlatform().getFlowConnector().connect( source, sink, pipe );
    }

  private Flow fourthFlow( Tap source, String path )
    {
    Pipe pipe = new Pipe( "fourth" );

    pipe = new Each( pipe, new Identity() );

    Tap sink = getPlatform().getTextFile( getOutputPath( path + "/fourth" ), SinkMode.REPLACE );

    return getPlatform().getFlowConnector().connect( source, sink, pipe );
    }

  public void testSimpleRiffle() throws IOException
    {
    getPlatform().copyFromLocal( inputFileIps );

    String path = "perpetual";

    Flow first = firstFlow( path );
    Flow second = secondFlow( first.getSink(), path );
    Flow third = thirdFlow( second.getSink(), path );
    Flow fourth = fourthFlow( third.getSink(), path );

    ProcessChain chain = new ProcessChain( true, fourth, second, first, third );

    chain.start();

    chain.complete();

    validateLength( fourth, 20 );
    }

  public void testSimpleRiffleCascade() throws IOException
    {
    getPlatform().copyFromLocal( inputFileIps );

    String path = "perpetualcascade";

    Flow first = firstFlow( path );
    Flow second = secondFlow( first.getSink(), path );
    Flow third = thirdFlow( second.getSink(), path );
    Flow fourth = fourthFlow( third.getSink(), path );

    ProcessFlow firstProcess = new ProcessFlow( "first", first );
    ProcessFlow secondProcess = new ProcessFlow( "second", second );
    ProcessFlow thirdProcess = new ProcessFlow( "third", third );
    ProcessFlow fourthProcess = new ProcessFlow( "fourth", fourth );

    Cascade cascade = new CascadeConnector().connect( fourthProcess, secondProcess, firstProcess, thirdProcess );

    cascade.start();

    cascade.complete();

    validateLength( fourth, 20 );
    }
  }
