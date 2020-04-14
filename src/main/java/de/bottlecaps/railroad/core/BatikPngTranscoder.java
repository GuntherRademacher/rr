package de.bottlecaps.railroad.core;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringReader;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XdmNode;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

public class BatikPngTranscoder implements PngTranscoder
{
  @Override
  public void transcode(XdmNode e, OutputStream o) throws Exception {
    String svg = new Processor(new Configuration()).newSerializer().serializeNodeToString(e);
    TranscoderInput input = new TranscoderInput(new StringReader(svg));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    TranscoderOutput output = new TranscoderOutput(baos);
    PNGTranscoder t = new PNGTranscoder();
    t.transcode(input, output);
    o.write(baos.toByteArray());
  }
}
