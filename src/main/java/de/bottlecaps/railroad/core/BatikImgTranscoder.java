package de.bottlecaps.railroad.core;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.function.Supplier;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;

enum BatikImgTranscoder implements ImgTranscoder {
  PNG(PNGTranscoder::new);

  private final Supplier<ImageTranscoder> constructor;

  private BatikImgTranscoder(Supplier<ImageTranscoder> constructor) {
    this.constructor = constructor;
  }

  @Override
  public void transcode(String svg, OutputStream outputStream) throws Exception {
    TranscoderInput input = new TranscoderInput(new StringReader(svg));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    TranscoderOutput output = new TranscoderOutput(baos);
    ImageTranscoder t = constructor.get();
    t.transcode(input, output);
    outputStream.write(baos.toByteArray());
  }

}