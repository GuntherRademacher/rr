package de.bottlecaps.railroad.core;
import java.io.OutputStream;

import net.sf.saxon.s9api.XdmNode;

public interface PngTranscoder
{
  void transcode(XdmNode e, OutputStream o) throws Exception;
}
