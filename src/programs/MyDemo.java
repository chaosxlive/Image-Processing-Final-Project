package programs;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class MyDemo {
	private final String[] VALID_EXT = { "png", "jpg", "jpeg" };

	private final int MAX_ALPHA = 300;
	private final int MAX_BETA = 200;
	private final int MAX_GAMMA = 200;
	private final int MAX_LOW_THRESHOLD = 100;
	private final int RATIO = 3;
	private final int KERNEL_SIZE = 3;
	private final Size BLUR_SIZE = new Size(5, 5);

	private JFrame frame;
	private JPanel panelCtrl, panelP, panelAB, panelG, panelC, panelOps, panelA, panelB;

	private JLabel labelImgSrc, labelImgMod;

	private JCheckBox cbUsingGamma, cbCalcCanny;

	private JSlider sliderA, sliderB, sliderG, sliderC;
	private JLabel labelA, labelB, labelG, labelC;

	private JButton buttonOpen, buttonSave;

	private JFileChooser fcr;

	private Mat matImgSrc, matImgMod;
	private double valAlpha, valBeta, valGamma;
	int lowThresh;

	public MyDemo() {
		valAlpha = 1.0;
		valBeta = 0.0;
		valGamma = 1.0;
		lowThresh = 0;
		matImgMod = new Mat();

		frame = new JFrame("Image Processing");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		fcr = new JFileChooser(new File("./src/data/origin"));
		if (fcr.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
			System.out.println("Open failed.");
			System.exit(0);
		}
		matImgSrc = Imgcodecs.imread(fcr.getSelectedFile().getAbsolutePath());

		setupUI(frame.getContentPane());

		doLinearTransformation();

		frame.pack();
		frame.setVisible(true);
	}

	private void saveImg(String path, String ext) {
		if (!Arrays.stream(VALID_EXT).anyMatch(ext.toLowerCase()::equals)) {
			System.out.println("Not supported extension" + ext);
			return;
		}
		try {
			ImageIO.write((RenderedImage) HighGui.toBufferedImage(matImgMod), ext, new File(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setupUI(Container container) {
		panelCtrl = new JPanel();
		panelCtrl.setLayout(new BoxLayout(panelCtrl, BoxLayout.PAGE_AXIS));

		panelC = new JPanel();
		panelC.setLayout(new BorderLayout());

		cbCalcCanny = new JCheckBox("Doing Edge detection");
		cbCalcCanny.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (cbUsingGamma.isSelected()) {
					doGammaCorrection();
				} else {
					doLinearTransformation();
				}
				super.mouseClicked(e);
			}
		});
		panelC.add(cbCalcCanny, BorderLayout.WEST);

		sliderC = new JSlider(0, MAX_LOW_THRESHOLD, 0);
		sliderC.setMajorTickSpacing(10);
		sliderC.setMinorTickSpacing(5);
		sliderC.setPaintTicks(true);
		sliderC.setPaintLabels(true);
		sliderC.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				lowThresh = sliderC.getValue();
				labelC.setText(String.format("Current Threshold: %03d", sliderC.getValue()));
				if (!cbCalcCanny.isSelected()) {
					return;
				}

				if (cbUsingGamma.isSelected()) {
					doGammaCorrection();
				} else {
					doLinearTransformation();
				}
			}
		});
		panelC.add(sliderC, BorderLayout.CENTER);

		labelC = new JLabel("Current Threshold: 000");
		panelC.add(labelC, BorderLayout.EAST);
		panelCtrl.add(panelC);

		panelP = new JPanel(new BorderLayout());

		cbUsingGamma = new JCheckBox("Using gamma correction");
		cbUsingGamma.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (cbUsingGamma.isSelected()) {
					panelP.remove(panelAB);
					panelP.add(panelG, BorderLayout.CENTER);
					doGammaCorrection();
					frame.revalidate();
					frame.repaint();
					frame.pack();
				} else {
					panelP.remove(panelG);
					panelP.add(panelAB, BorderLayout.CENTER);
					doLinearTransformation();
					frame.revalidate();
					frame.repaint();
					frame.pack();
				}
			}
		});
		panelP.add(cbUsingGamma, BorderLayout.WEST);

		panelAB = new JPanel();
		panelAB.setLayout(new BoxLayout(panelAB, BoxLayout.PAGE_AXIS));

		// Alpha
		panelA = new JPanel(new BorderLayout());
		panelA.add(new JLabel("Alpha Gain  (Constast):"), BorderLayout.WEST);
		sliderA = new JSlider(0, MAX_ALPHA, 100);
		sliderA.setMajorTickSpacing(50);
		sliderA.setMinorTickSpacing(10);
		sliderA.setPaintTicks(true);
		sliderA.setPaintLabels(true);
		sliderA.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				labelA.setText(String.format("Current Alpha:     %03d", sliderA.getValue()));
				valAlpha = sliderA.getValue() / 100.0;
				doLinearTransformation();
			}
		});
		panelA.add(sliderA, BorderLayout.CENTER);
		labelA = new JLabel("Current Alpha:     100");
		panelA.add(labelA, BorderLayout.EAST);
		panelAB.add(panelA);

		// Beta
		panelB = new JPanel(new BorderLayout());
		panelB.add(new JLabel("Beta Bias (Brightness):"), BorderLayout.WEST);
		sliderB = new JSlider(0, MAX_BETA, 100);
		sliderB.setMajorTickSpacing(20);
		sliderB.setMinorTickSpacing(5);
		sliderB.setPaintTicks(true);
		sliderB.setPaintLabels(true);
		sliderB.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				labelB.setText(String.format("Current Beta:      %03d", sliderB.getValue()));
				valBeta = sliderB.getValue() - 100;
				doLinearTransformation();
				super.mouseReleased(e);
			}
		});
		panelB.add(sliderB, BorderLayout.CENTER);
		labelB = new JLabel("Current Beta:      100");
		panelB.add(labelB, BorderLayout.EAST);
		panelAB.add(panelB);

		panelP.add(panelAB, BorderLayout.CENTER);

		panelCtrl.add(panelP);

		panelG = new JPanel(new BorderLayout());
		sliderG = new JSlider(0, MAX_GAMMA, 100);
		sliderG.setMajorTickSpacing(20);
		sliderG.setMinorTickSpacing(5);
		sliderG.setPaintTicks(true);
		sliderG.setPaintLabels(true);
		sliderG.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				labelG.setText(String.format("Current Gamma: %03d", sliderG.getValue()));
				valGamma = sliderG.getValue() / 100.0;
				doGammaCorrection();
				super.mouseReleased(e);
			}
		});
		panelG.add(sliderG, BorderLayout.CENTER);
		labelG = new JLabel("Current Gamma: 100");
		panelG.add(labelG, BorderLayout.EAST);

		panelOps = new JPanel(new FlowLayout());
		buttonOpen = new JButton("Open");
		buttonOpen.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				fcr.setCurrentDirectory((new File("./src/data/origin")).getAbsoluteFile());
				if (fcr.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					matImgSrc = Imgcodecs.imread(fcr.getSelectedFile().getAbsolutePath());
					labelImgSrc.setIcon(scaleImage(new ImageIcon(HighGui.toBufferedImage(matImgSrc))));
					labelImgMod.setIcon(scaleImage(new ImageIcon(HighGui.toBufferedImage(matImgSrc))));

					if (cbUsingGamma.isSelected()) {
						doGammaCorrection();
					} else {
						doLinearTransformation();
					}

					frame.repaint();
				}
				super.mouseClicked(e);
			}
		});
		buttonSave = new JButton("Save");
		buttonSave.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				fcr.setCurrentDirectory((new File("./src/data/result")).getAbsoluteFile());
				if (fcr.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
					String filename = fcr.getSelectedFile().getName();
					String ext = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
					saveImg(fcr.getSelectedFile().getAbsolutePath(), ext);
				}
				super.mouseClicked(e);
			}
		});
		panelOps.add(buttonOpen);
		panelOps.add(buttonSave);
		panelCtrl.add(panelOps);

		container.add(panelCtrl, BorderLayout.PAGE_START);
		JPanel panelMain = new JPanel();
		panelMain.setLayout(new FlowLayout());
		labelImgSrc = new JLabel(scaleImage(new ImageIcon(HighGui.toBufferedImage(matImgSrc))));
		panelMain.add(labelImgSrc);
		labelImgMod = new JLabel(scaleImage(new ImageIcon(HighGui.toBufferedImage(matImgSrc))));
		panelMain.add(labelImgMod);
		container.add(panelMain, BorderLayout.CENTER);
	}

	private void doLinearTransformation() {
		matImgSrc.convertTo(matImgMod, -1, valAlpha, valBeta);

		if (cbCalcCanny.isSelected()) {
			doEdgeDetection();
		}

		labelImgMod.setIcon(scaleImage(new ImageIcon(HighGui.toBufferedImage(matImgMod))));
		frame.repaint();
	}

	private byte saturate(double dVal) {
		int iVal = (int) Math.round(dVal);
		iVal = iVal > 255 ? 255 : (iVal < 0 ? 0 : iVal);

		return (byte) iVal;
	}

	private void doGammaCorrection() {
		Mat lookupTable = new Mat(1, 256, CvType.CV_8U);
		byte[] lookupTableData = new byte[(int) (lookupTable.total() * lookupTable.channels())];

		for (int i = 0; i < lookupTable.cols(); i++) {
			lookupTableData[i] = saturate(Math.pow(i / 255.0, valGamma) * 255.0);
		}

		lookupTable.put(0, 0, lookupTableData);
		Core.LUT(matImgSrc, lookupTable, matImgMod);

		if (cbCalcCanny.isSelected()) {
			doEdgeDetection();
		}

		labelImgMod.setIcon(scaleImage(new ImageIcon(HighGui.toBufferedImage(matImgMod))));
		frame.repaint();
	}

	private void doEdgeDetection() {
		Mat src = matImgMod.clone(), matBlur = new Mat(), matEdge = new Mat(), dst;
		Imgproc.blur(src, matBlur, BLUR_SIZE);
		Imgproc.Canny(matBlur, matEdge, lowThresh, lowThresh * RATIO, KERNEL_SIZE, true);
		dst = new Mat(matImgSrc.size(), CvType.CV_8U, Scalar.all(0));
		src.copyTo(dst, matEdge);
		matImgMod = dst;
	}

	private ImageIcon scaleImage(ImageIcon icon) {
		int w = 800, h = 800;
		int nw = icon.getIconWidth();
		int nh = icon.getIconHeight();

		if (icon.getIconWidth() > w) {
			nw = w;
			nh = (nw * icon.getIconHeight()) / icon.getIconWidth();
		}

		if (nh > h) {
			nh = h;
			nw = (icon.getIconWidth() * nh) / icon.getIconHeight();
		}

		return new ImageIcon(icon.getImage().getScaledInstance(nw, nh, Image.SCALE_SMOOTH));
	}

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new MyDemo();
			}
		});
	}

}
