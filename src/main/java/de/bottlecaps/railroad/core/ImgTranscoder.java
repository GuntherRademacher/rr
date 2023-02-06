package de.bottlecaps.railroad.core;
import java.io.OutputStream;

public interface ImgTranscoder
{
  void transcode(String svg, OutputStream os) throws Exception;
}
