package com.horizen.box;

import com.horizen.proposition.Proposition;

/**
 * Just to not farget what is a Box interface
 *
 trait Box[P <: Proposition] extends BytesSerializable {
 val value: Box.Amount
 val proposition: P

 val id: ADKey
 }

 object Box {
 type Amount = Long
 }

 */


public interface Box<P extends Proposition> extends scorex.core.transaction.box.Box<P>
{
    @Override
    long value();

    @Override
    P proposition();

    @Override
    byte[] id();

    @Override
    byte[] bytes();

    @Override
    BoxSerializer serializer();

    scorex.core.ModifierTypeId boxTypeId();
}