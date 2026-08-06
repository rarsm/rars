package rars.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Observable;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import rars.Globals;
import rars.ProgramStatement;
import rars.assembler.Symbol;
import rars.riscv.Instruction;
import rars.riscv.hardware.AccessNotice;
import rars.riscv.hardware.AddressErrorException;
import rars.riscv.hardware.Memory;
import rars.riscv.hardware.MemoryAccessNotice;
import rars.riscv.hardware.RegisterAccessNotice;
import rars.riscv.hardware.RegisterFile;
import rars.riscv.instructions.JAL;
import rars.riscv.instructions.JALR;
import rars.riscv.instructions.Store;

/**
 * Tool for analyzing the call stack at runtime.
 */
public class CallstackAnalyzer extends AbstractToolAndApplication {
	private static final String NAME = "Callstack Analyzer";
	private static final String VERSION = "Version 1.0";
	private static final String HEADING = "Runtime analysis of the call stack";
	
	private static final int STACK_BASE =
			(int) RegisterFile.getRegister("sp").getResetValue();
	
	private static boolean altFramePointerName;
	
	private EventLogger eventLogger = new EventLogger();
	private int stackPointer = (int) RegisterFile.getRegister("sp").getResetValue();
	private int framePointer = (int) RegisterFile.getRegister("fp").getResetValue();
	private int programCounter = RegisterFile.getInitialProgramCounter();
	private List<StackAnalyzer> stackAnalyzers = List.of(
			new StackAnalyzer(new StackPointerAnalyzer()),
			new StackAnalyzer(new SingleFrameAnalyzer()),
			new StackAnalyzer(new CallReturnAnalyzer()),
			new StackAnalyzer(new UnwindingAnalyzer()));
	
	private JTextArea eventLogArea;
	private JLabel stackPointerLabel;
	private JLabel framePointerLabel;
	private JLabel programCounterLabel;
	private List<StackDiagram> stackDiagrams;
	private long lastUpdateMillis = 0;

	public CallstackAnalyzer() {
		super(NAME + ", " + VERSION, HEADING);
	}
	
	public static void main(String[] args) {
		new CallstackAnalyzer().go();
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	protected JComponent buildMainDisplayArea() {
		Font monospacedFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);
		
		JSplitPane root = new JSplitPane();
		root.setPreferredSize(new Dimension(800, 500));
		
		JPanel left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
		
		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
		settingsPanel.setMaximumSize(
				new Dimension(Integer.MAX_VALUE, settingsPanel.getPreferredSize().height));
		settingsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		settingsPanel.setBorder(BorderFactory.createTitledBorder("Analyzer Settings"));
		JCheckBox altFramePointerNameCheckBox = new JCheckBox("use fp as alternative name for s0");
		altFramePointerNameCheckBox.addActionListener(e -> {
			altFramePointerName = altFramePointerNameCheckBox.isSelected();
		});
		altFramePointerNameCheckBox.doClick(); // select and invoke event handler
		settingsPanel.add(altFramePointerNameCheckBox);
		left.add(settingsPanel);
		
		eventLogArea = new JTextArea();
		eventLogArea.setEditable(false);
		eventLogArea.setFont(monospacedFont);
		eventLogArea.setLineWrap(true);
		JScrollPane eventLogAreaScroll = new JScrollPane(eventLogArea,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		eventLogAreaScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		left.add(eventLogAreaScroll);
		
		Dimension size;
		JPanel registersPanel = new JPanel();
		registersPanel.setLayout(new BoxLayout(registersPanel, BoxLayout.Y_AXIS));
		JPanel stackPointerPanel = new JPanel();
		stackPointerPanel.setLayout(new BoxLayout(stackPointerPanel, BoxLayout.X_AXIS));
		stackPointerLabel = new JLabel(" ");
		stackPointerLabel.setFont(monospacedFont);
		stackPointerPanel.add(stackPointerLabel);
		size = stackPointerPanel.getPreferredSize();
		size.width = Integer.MAX_VALUE;
		stackPointerPanel.setMaximumSize(size);
		registersPanel.add(stackPointerPanel);
		JPanel framePointerPanel = new JPanel();
		framePointerPanel.setLayout(new BoxLayout(framePointerPanel, BoxLayout.X_AXIS));
		framePointerLabel = new JLabel(" ");
		framePointerLabel.setFont(monospacedFont);
		framePointerPanel.add(framePointerLabel);
		size = framePointerPanel.getPreferredSize();
		size.width = Integer.MAX_VALUE;
		framePointerPanel.setMaximumSize(size);
		registersPanel.add(framePointerPanel);
		JPanel programCounterPanel = new JPanel();
		programCounterPanel.setLayout(new BoxLayout(programCounterPanel, BoxLayout.X_AXIS));
		programCounterLabel = new JLabel(" ");
		programCounterLabel.setFont(monospacedFont);
		programCounterPanel.add(programCounterLabel);
		size = programCounterPanel.getPreferredSize();
		size.width = Integer.MAX_VALUE;
		programCounterPanel.setMaximumSize(size);
		registersPanel.add(programCounterPanel);
		left.add(registersPanel);
		
		root.setLeftComponent(left);
		
		JPanel right = new JPanel();
		right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
		stackDiagrams = new ArrayList<>();
		for (StackAnalyzer analyzer : stackAnalyzers) {
			StackDiagram diagram = new StackDiagram();
			diagram.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
			diagram.setBase(STACK_BASE);
			JScrollPane scrollPane = new JScrollPane(diagram,
					JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			String title = analyzer.getAnalyzer().getDescription();
			scrollPane.setBorder(BorderFactory.createTitledBorder(title));
			String text = "strategy: "+ analyzer.getAnalyzer().getDescription();
			JCheckBox checkBox = new JCheckBox(text);
			checkBox.addActionListener(e -> {
				if (checkBox.isSelected()) {
					right.add(scrollPane);
				} else {
					right.remove(scrollPane);
				}
				root.revalidate();
			});
			if (stackDiagrams.isEmpty()) {
				checkBox.doClick(); // select and invoke event handler
			}
			settingsPanel.add(checkBox);
			stackDiagrams.add(diagram);
		}
		root.setRightComponent(right);
		
		return root;
	}
	
	@Override
	protected void addAsObserver() {
		// connect
		addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
		addAsObserver(Memory.stackLimitAddress, Memory.stackBaseAddress);
		addAsObserver(RegisterFile.getRegister("sp"));
		addAsObserver(RegisterFile.getRegister("fp"));
		addAsObserver(RegisterFile.getProgramCounterRegister());
	}
	
	@Override
	protected void deleteAsObserver() {
		// disconnect
		super.deleteAsObserver();
		deleteAsObserver(RegisterFile.getRegister("sp"));
		deleteAsObserver(RegisterFile.getRegister("fp"));
		deleteAsObserver(RegisterFile.getProgramCounterRegister());
	}

	@Override
	public void processRISCVUpdate(Observable resource, AccessNotice notice) {
		switch (notice) {
		case RegisterAccessNotice regNotice -> {
			if (regNotice.getAccessType() == AccessNotice.WRITE) {
				String name = regNotice.getRegisterName();
				switch (name) {
				case "sp" -> {
					stackPointer = (int) RegisterFile.getRegister(name)
							.getValueNoNotify();
					addEvent(new SetStackPointer(stackPointer));
				}
				case "s0" -> {
					framePointer = (int) RegisterFile.getRegister(name)
							.getValueNoNotify();
					addEvent(new SetFramePointer(framePointer));
				}
				case "pc" -> {
					programCounter = (int) RegisterFile.getProgramCounterRegister()
							.getValueNoNotify();
				}
				}
			}
		}
		
		case MemoryAccessNotice memNotice -> {
			int address = memNotice.getAddress();
			if (memNotice.getAccessType() == AccessNotice.READ
					&& memNotice.accessIsFromRISCV()
					&& address >= Memory.textBaseAddress
					&& address < Memory.textLimitAddress) {
				try {
					ProgramStatement statement =
							Memory.getInstance().getStatementNoNotify(address);
					if (statement != null) {
						Instruction instruction = statement.getInstruction();
						switch (instruction) {
						case JAL jal -> {
							int returnRegister = statement.getOperand(0);
							if (returnRegister == RegisterFile.getRegister("ra").getNumber()) {
								addEvent(new ExecuteCall());
							}
						}
						case JALR jalr -> {
							int returnRegister = statement.getOperand(0);
							int targetRegister = statement.getOperand(1);
							// CAUTOIN: offset is not yet sign extended
							int offset = statement.getOperand(2);
							if (returnRegister == RegisterFile.getRegister("ra").getNumber()) {
								addEvent(new ExecuteCall());
							} else if (targetRegister == RegisterFile.getRegister("ra").getNumber()
									&& offset == 0) {
								addEvent(new ExecuteReturn());
							}
						}
						case Store store -> {
							int offset = (statement.getOperand(1) << 20) >> 20;
							int upper = RegisterFile.getValueNoNotify(statement.getOperand(2));
							int source = statement.getOperand(0);
							int value = RegisterFile.getValueNoNotify(source);
							addEvent(new ExecuteStore(offset + upper, source, value));
						}
						default -> {}
						}
					}
				} catch (AddressErrorException e) {
					e.printStackTrace();
				}
			} else if (memNotice.getAccessType() == AccessNotice.WRITE
					&& address <= Memory.stackBaseAddress
					&& address >= stackPointer) {
				addEvent(new WriteStackValue(address, memNotice.getLength(), memNotice.getValue()));
			}
		}
		
		default -> {}
		}
	}
	
	private void addEvent(Event event) {
		eventLogger.log(event);
		for (StackAnalyzer analyzer : stackAnalyzers) {
			analyzer.apply(event);
		}
	}
	
	@Override
	protected void updateDisplay() {
		int selectStart = eventLogArea.getSelectionStart();
		int selectEnd = eventLogArea.getSelectionEnd();
		boolean follow = selectStart == selectEnd && selectStart == eventLogArea.getText().length();
		eventLogArea.setText(eventLogger.getText());
		eventLogArea.setSelectionEnd(follow ? eventLogArea.getText().length() : selectEnd);
		eventLogArea.setSelectionStart(follow ? eventLogArea.getText().length() : selectStart);

		stackPointerLabel.setText(String.format("sp = 0x%08x", stackPointer));
		framePointerLabel.setText(String.format("%s = 0x%08x",
				altFramePointerName ? "fp" : "s0", framePointer));
		programCounterLabel.setText(String.format("pc = 0x%08x", programCounter));
		
		Iterator<StackDiagram> iter = stackDiagrams.iterator();
		for (StackAnalyzer analyzer : stackAnalyzers) {
			StackDiagram diagram = iter.next();
			diagram.setStackPointer(stackPointer);
			diagram.setFramePointer(framePointer);
			diagram.setProgramCounter(programCounter);
			diagram.setFrames(analyzer.getFrames());
		}
	}
	
	@Override
	protected void reset() {
		eventLogger.clear();
		for (StackAnalyzer analyzer : stackAnalyzers) {
			analyzer.reset();
		}
		
		stackPointer = (int) RegisterFile.getRegister("sp").getResetValue();
		framePointer = (int) RegisterFile.getRegister("fp").getResetValue();
		
		updateDisplay();
	}
	
	@Override
	protected JComponent getHelpComponent() {
		JButton helpButton = new JButton("Help");
		helpButton.addActionListener(e -> {
			HelpDialog helpDialog = new HelpDialog();
			helpDialog.setLocationRelativeTo(null);
			helpDialog.setVisible(true);
		});
		return helpButton;
	}
	
	private static String registerName(int index) {
		String name = RegisterFile.getRegisters()[index].getName();
		return altFramePointerName && name == "s0" ? "fp" : name;
	}
	
	private static Optional<String> formatSymbol(int address, boolean showOffset) {
		Optional<Symbol> optSymbol = Globals.program.getLocalSymbolTable().getAllSymbols().stream()
				.sorted((a, b) -> Integer.compareUnsigned(b.getAddress(), a.getAddress()))
				.filter(s -> Integer.compareUnsigned(s.getAddress(), address) <= 0)
				.findFirst();
		if (optSymbol.isEmpty()) {
			return Optional.empty();
		}
		Symbol symbol = optSymbol.get();
		boolean isDataSymbol = symbol.getType();
		if (isDataSymbol && !Memory.inDataSegment(address)
				|| !isDataSymbol && !Memory.inTextSegment(address)) {
			return Optional.empty();
		}
		long offset = Integer.toUnsignedLong(address) - Integer.toUnsignedLong(symbol.getAddress());
		if (offset == 0 || !showOffset) {
			return Optional.of(symbol.getName());
		} else {
			return Optional.of(String.format("%s+0x%x", symbol.getName(), offset));
		}
	}
	
	private static class EventLogger {
		private List<Event> events = new ArrayList<>();
		private StringBuilder text = new StringBuilder();
		
		public synchronized void clear() {
			events.clear();
			text = new StringBuilder();
		}
		
		public synchronized void log(Event event) {
			events.add(event);
			text.append(event);
			text.append('\n');
		}
		
		public synchronized String getText() {
			return text.toString();
		}
	}
	
	private static sealed interface Event permits SetStackPointer, SetFramePointer, WriteStackValue,
			ExecuteCall, ExecuteReturn, Message, ExecuteStore {}
	
	private static record SetStackPointer(int value) implements Event {
		@Override
		public String toString() {
			return String.format("sp = 0x%08x", value);
		}
	}
	
	private static record SetFramePointer(int value) implements Event {
		@Override
		public String toString() {
			return String.format("%s = 0x%08x", altFramePointerName ? "fp" : "s0", value);
		}
	}
	
	private static record WriteStackValue(int address, int bytes, int value) implements Event {
		@Override
		public String toString() {
			return String.format("write stack %d bytes 0x%08x to 0x%08x",
					bytes, value, address);
		}
	}
	
	private static record ExecuteCall() implements Event {
		@Override
		public final String toString() {
			return String.format("call");
		}
	}
	
	private static record ExecuteReturn() implements Event {
		@Override
		public final String toString() {
			return String.format("return");
		}
	}
	
	private static record Message(Severity severity, String message) implements Event {
		@Override
		public final String toString() {
			return String.format("%s: %s", severity, message);
		}
		
		private static enum Severity {
			INFO, WARNING;
		}
	}
	
	private static record ExecuteStore(int address, int register, int value) implements Event {
		@Override
		public String toString() {
			return String.format("store %s (0x%08x) to 0x%08x",
					registerName(register), value, address);
		}
	}
	
	private static record Write(int address, int bytes, int value, Optional<String> source) {
		public boolean overlapsWith(Write other) {
			int otherLast = other.address + other.bytes - 1;
			return (other.address >= address && other.address < address + bytes)
					|| (otherLast >= address && otherLast < address + bytes);
		}

		@Override
		public String toString() {
			return String.format("Write [address=0x%08x, bytes=%d, value=0x%08x, source=%s]",
					address, bytes, value, source);
		}
	}
	
	private static class Frame {
		private int base;
		private int top;
		private List<Write> writes = new ArrayList<>();
		
		public Frame(int base, int top) {
			this.base = base;
			this.top = top;
		}
		
		public Frame(Frame frame) {
			this(frame.base, frame.top);
		}
		
		public int getBase() {
			return base;
		}
		
		public void setBase(int base) {
			this.base = base;
			removeNotContainedWrites();
		}
		
		public int getTop() {
			return top;
		}
		
		public void setTop(int top) {
			this.top = top;
			removeNotContainedWrites();
		}
		
		public List<Write> getWrites() {
			// copy necessary to avoid ConcurrentModificationException while drawing
			return List.copyOf(writes);
		}
		
		public void write(Write write) {
			Iterator<Write> iter = writes.iterator();
			while (iter.hasNext()) {
				Write w = iter.next();
				if (w.overlapsWith(write)) {
					iter.remove();
				}
			}
			writes.add(write);
		}
		
		public Optional<Integer> findReturnAddress() {
			return writes.stream()
					.filter(write -> write.source().equals(Optional.of("ra")))
					.map(write -> write.value())
					.findAny();
		}
		
		public boolean containsAddress(int address) {
			return address >= top && address < base;
		}
		
		public boolean containsWrite(Write write) {
			return write.address() < base && write.address() + write.bytes() > top;
		}
		
		private void removeNotContainedWrites() {
			Iterator<Write> iter = writes.iterator();
			while (iter.hasNext()) {
				Write write = iter.next();
				if (!containsWrite(write)) {
					iter.remove();
				}
			}
		}

		@Override
		public String toString() {
			return String.format("Frame [base=STACK_BASE-0x%08x, top=base-0x%08x]",
					STACK_BASE - base, base - top);
		}

		@Override
		public int hashCode() {
			return Objects.hash(base, top);
		}

		/**
		 * Two frames are considered equal if their base address and top address match.
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Frame other = (Frame) obj;
			return base == other.base && top == other.top;
		}
	}
	
	private static class StackAnalyzer {
		private List<Frame> frames = new ArrayList<>();
		private Optional<ExecuteStore> lastStore = Optional.empty();
		private EventAnalyzer analyzer;
		
		public StackAnalyzer(EventAnalyzer analyzer) {
			this.analyzer = analyzer;
		}
		
		public EventAnalyzer getAnalyzer() {
			return analyzer;
		}
		
		public void reset() {
			analyzer.reset();
			frames.clear();
			lastStore = Optional.empty();
		}
		
		public List<Frame> getFrames() {
			return frames;
		}
		
		public List<Message> apply(Event event) {
			List<Message> messages = new ArrayList<>();
			
			if (frames.size() == 0) {
				frames.add(new Frame(STACK_BASE, STACK_BASE));
			}
			
			switch (event) {
			case SetStackPointer(int value) -> {
				if (value < Memory.stackLimitAddress || value >= Memory.stackBaseAddress) {
					messages.add(new Message(Message.Severity.WARNING, String.format(
							"stack pointer 0x%08x outside stack segment", value)));
				}
			}
			case SetFramePointer(int value) -> {
				if (value < Memory.stackLimitAddress || value >= Memory.stackBaseAddress) {
					messages.add(new Message(Message.Severity.WARNING, String.format(
							"frame pointer 0x%08x outside stack segment", value)));
				}
			}
			case WriteStackValue(int address, int bytes, int value) -> {
				Optional<String> source = lastStore.flatMap(store -> {
					return store.address() == address
							? Optional.of(registerName(store.register()))
							: Optional.empty();
				});
				frames.stream()
						.filter(frame -> frame.containsAddress(address))
						.findFirst().ifPresentOrElse(frame -> {
							frame.write(new Write(address, bytes, value, source));
						}, () -> {
							messages.add(new Message(Message.Severity.WARNING, String.format(
									"Write to stack segment outside all frames at 0x%08x.",
									address)));
						});
			}
			case ExecuteStore store -> {
				lastStore = Optional.of(store);
			}
			default -> {}
			}
			
			messages.addAll(analyzer.apply(this, event));
			
			return messages;
		}
	}
	
	private static interface EventAnalyzer {
		String getDescription();
		
		void reset();
		
		List<Message> apply(StackAnalyzer analyzer, Event event);
	}
	
	private static class SingleFrameAnalyzer implements EventAnalyzer {
		@Override
		public String getDescription() {
			return "no frames";
		}
		
		@Override
		public void reset() {
		}
		
		@Override
		public List<Message> apply(StackAnalyzer analyzer, Event event) {
			List<Frame> frames = analyzer.getFrames();
			
			switch (event) {
			case SetStackPointer(int value) -> {
				frames.getFirst().setTop(value);
			}
			default -> {}
			}
			
			return List.of();
		}
	}
	
	private static class StackPointerAnalyzer implements EventAnalyzer {
		@Override
		public String getDescription() {
			return "only stack pointer";
		}
		
		@Override
		public void reset() {
		}
		
		@Override
		public List<Message> apply(StackAnalyzer analyzer, Event event) {
			List<Frame> frames = analyzer.getFrames();
			switch (event) {
			case SetStackPointer(int value) -> {
				Frame lastFrame = null;
				while (frames.size() > 1 && value > frames.getFirst().getTop()) {
					lastFrame = frames.removeFirst();
				}
				if (value < frames.getFirst().getTop()) {
					Frame frame = new Frame(frames.getFirst().getTop(), value);
					if (lastFrame != null) {
						// restore writes if frame was only partially popped
						lastFrame.getWrites().forEach(write -> {
							if (frame.containsWrite(write)) {
								frame.write(write);
							}
						});
					}
					frames.addFirst(frame);
				}
			}
			default -> {}
			}
			
			return List.of();
		}
	}
	
	private static class CallReturnAnalyzer implements EventAnalyzer {
		@Override
		public String getDescription() {
			return "call and return";
		}
		
		@Override
		public void reset() {
		}
		
		@Override
		public List<Message> apply(StackAnalyzer analyzer, Event event) {
			List<Message> messages = new ArrayList<>();
			List<Frame> frames = analyzer.getFrames();
			
			switch (event) {
			case SetStackPointer(int value) -> {
				frames.getFirst().setTop(value);
			}
			case SetFramePointer(int value) -> {
				frames.getFirst().setBase(value);
			}
			case ExecuteCall() -> {
				frames.addFirst(new Frame(frames.getFirst()));
			}
			case ExecuteReturn() -> {
				Frame frame = frames.removeFirst();
				if (frames.isEmpty()) {
					messages.add(new Message(Message.Severity.WARNING,
							"Return from function which was not entered with call."));
					break;
				}
				Frame previous = frames.getFirst();
				if (!frame.equals(previous)) {
					messages.add(new Message(Message.Severity.WARNING,
							"Previous stack frame not restored after return."));
				}
			}
			default -> {}
			}
			
			return messages;
		}
	}
	
	private static class UnwindingAnalyzer implements EventAnalyzer {
		private int stackPointer = STACK_BASE;
		private int framePointer = STACK_BASE;
		
		@Override
		public String getDescription() {
			return "stack unwinding";
		}
		
		@Override
		public void reset() {
			stackPointer = STACK_BASE;
			framePointer = STACK_BASE;
		}
		
		@Override
		public List<Message> apply(StackAnalyzer analyzer, Event event) {
			List<Frame> frames = analyzer.getFrames();
			
			boolean update = false;
			switch (event) {
			case SetStackPointer(int value) -> {
				stackPointer = value;
				update = true;
			}
			case SetFramePointer(int value) -> {
				framePointer = value;
				update = true;
			}
			default -> {}
			}
			if (update) {
				List<Frame> found = new ArrayList<>();
				int sp = stackPointer;
				int fp = framePointer;
				for (int i = 0; i < 5; i++) {
					found.add(new Frame(fp, sp));
					// expect ra at -4(fp), expect s0/fp at -8(fp)
					try {
						int nextFp = Memory.getInstance().getWordNoNotify(fp-8);
						if (fp == nextFp || fp == STACK_BASE) {
							break;
						}
						if (nextFp > Memory.stackBaseAddress || nextFp < Memory.stackLimitAddress) {
							break;
						}
						sp = fp;
						fp = nextFp;
					} catch (AddressErrorException e) {
						e.printStackTrace();
						break;
					}
				}
				
				List<Write> writes = frames.stream()
						.flatMap(frame -> frame.getWrites().stream())
						.toList();
				for (Write write : writes) {
					for (Frame frame : found) {
						if (frame.containsWrite(write)) {
							frame.write(write);
						}
					}
				}
				frames.clear();
				frames.addAll(found);
			}
			
			return List.of();
		}
	}
	
	private static class StackDiagram extends JPanel {
		private int baseAddr = 0;
		/** Originally indented to be used for adjustable size. Now fixed value. */
		private final int scale = 4;
		/** width of the center diagram */
		private int stackWidth = 100;
		/** padding of left text info */
		private int stackLPad = 4;
		/** padding of right text info */
		private int stackRPad= 4;
		/** space between the left border and the left edge of the center diagram */
		private int stackLeft = 0;
		/** space between the right border and the right edge of the center diagram */
		private int stackRight = 0;
		/** space between the bottom border and the bottom edge of the center diagram */
		private int stackBottom = 0;
		private Font normalFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);
		private Font smallFont = new Font(Font.MONOSPACED, Font.PLAIN, 9);
		private List<Frame> frames = List.of();
		private int stackPointer;
		private int framePointer;
		private int programCounter;
		
		public StackDiagram() {
			super(null);
			setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
			update();
		}
		
		public void setBase(int base) {
			this.baseAddr = base;
			update();
		}
		
		public void setStackPointer(int sp) {
			stackPointer = sp;
		}
		
		public void setFramePointer(int fp) {
			framePointer = fp;
		}
		
		public void setProgramCounter(int pc) {
			programCounter = pc;
		}
		
		public void setFrames(List<Frame> frames) {
			this.frames = List.copyOf(frames);
			update();
		}
		
		private void update() {
			Insets insets = getInsets();
			int top = frames.stream().mapToInt(Frame::getTop).min().orElse(baseAddr);
			int width = insets.left + stackLeft + stackWidth + stackRight + insets.right;
			int height = insets.bottom + stackBottom + (baseAddr - top) * scale + insets.top;
			Dimension size = getPreferredSize();
			if (size.width < width || size.height < height) {
				size.width = size.width < width ? width : size.width;
				size.height = size.height < height ? height : size.height;
				setPreferredSize(size);
				revalidate();
			}
			repaint();
		}
		
		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			
			if (frames.isEmpty()) return;
			
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			
			 // always show stack base address at the bottom
			drawAddr(g, STACK_BASE, Optional.empty());

			if (stackPointer == framePointer) {
				drawAddr(g, stackPointer, Optional.of("sp " + (altFramePointerName ? "fp" : "s0")));
			} else {
				drawAddr(g, stackPointer, Optional.of("sp"));
				drawAddr(g, framePointer, Optional.of(altFramePointerName ? "fp" : "s0"));
			}
			
			boolean top = true;
			Optional<Integer> prevReturnAddress = Optional.empty();
			for (Frame frame : frames) {
				Optional<String> name;
				if (top) {
					name = formatSymbol(programCounter, false);
				} else {
					name = prevReturnAddress.flatMap(ra -> formatSymbol(ra, false));
				}
				drawFrame(g, frame, top, name);
				drawAddr(g, frame.getTop(), Optional.empty());
				if (frame.getBase() - frame.getTop() != 0) {
					drawFrameSize(g, frame);
				}
				prevReturnAddress = frame.findReturnAddress();
				top = false;
			}

			drawOffsetLabel(g, 0);
			drawOffsetLabel(g, 1);
			drawOffsetLabel(g, 2);
			drawOffsetLabel(g, 3);
		}
		
		private int addrToY(int addr) {
			return getHeight() - getInsets().bottom - stackBottom - (baseAddr - addr) * scale;
		}
		
		private void drawOffsetLabel(Graphics2D g, int offset) {
			g.setFont(normalFont);
			
			String text = "+" + offset;
			Rectangle2D textBounds = g.getFontMetrics().getStringBounds(text, g);
			int textHeight = (int) Math.ceil(textBounds.getWidth());
			if (textHeight > stackBottom) {
				stackBottom = textHeight;
				update();
			}

			Insets insets = getInsets();
			int top = getHeight() - insets.bottom - stackBottom;
			int right = insets.left + stackLeft + stackWidth * (offset + 1) / 4;
			g.drawString(text,
					right - (int) Math.ceil(textBounds.getWidth()),
					top - (int) Math.ceil(textBounds.getY()));
			g.drawLine(right, top, right, top - (int) Math.ceil(textBounds.getY()));
			g.drawLine(right - stackWidth / 4, top,
					right - stackWidth / 4, top - (int) Math.ceil(textBounds.getY()));
		}
		
		private void drawFrame(Graphics2D g, Frame frame, boolean current, Optional<String> name) {
			g.setFont(normalFont);
			
			Insets insets = getInsets();
			int bottom = addrToY(frame.getBase());
			int frameHeight = (frame.getBase() - frame.getTop()) * scale;
			
			g.setColor(Color.GRAY);
			g.fillRect(insets.left + stackLeft, bottom - frameHeight, stackWidth, frameHeight);
			
			if (name.isPresent()) {
				drawFrameName(g, frame, name.get());
			}
			
			for (Write w : frame.getWrites()) {
				drawWrite(g, w, current);
			}
			
			g.setColor(Color.BLACK);
			Stroke savedStroke = g.getStroke();
			g.setStroke(new BasicStroke(2));
			g.drawRect(insets.left + stackLeft, bottom - frameHeight, stackWidth, frameHeight);
			g.setStroke(savedStroke);
		}
		
		private void drawWrite(Graphics2D g, Write write, boolean current) {
			g.setFont(normalFont);
			
			Insets insets = getInsets();
			int wordAddr = write.address() >> 2 << 2;
			int wordOffset = write.address() - wordAddr;
			int left = insets.left + stackLeft + stackWidth * wordOffset / 4;
			int top = addrToY(wordAddr);
			
			g.setColor(current ? Color.GREEN : Color.YELLOW);
			g.fillRect(left, top, stackWidth * write.bytes() / 4, 4 * scale);
			g.setColor(Color.BLACK);
			g.drawRect(left, top, stackWidth * write.bytes() / 4, 4 * scale);
			g.setColor(Color.BLACK);
			
			int value = write.value();
			for (int i = 0; i < write.bytes(); i++) {
				String text = String.format("%02x", (value >> (8 * i)) & 0xff);
				Rectangle2D bounds = g.getFontMetrics().getStringBounds(text, g);
				g.drawString(text,
						left + stackWidth * (i + 1) / 4 - (int) Math.ceil(bounds.getWidth()),
						top - (int) Math.ceil(bounds.getY()));
			}
			
			drawWriteSource(g, write);
		}
		
		private void drawWriteSource(Graphics2D g, Write write) {
			
			write.source().ifPresent(source -> {
				Insets insets = getInsets();
				
				int top = addrToY(write.address() >> 2 << 2);
				
				Optional<String> symbol = formatSymbol(write.value(), true);
				String text1 = String.format("%s = 0x%08x", source, write.value());
				g.setFont(normalFont);
				Rectangle2D bounds1 = g.getFontMetrics().getStringBounds(text1, g);
				int textWidth1 = (int) Math.ceil(bounds1.getWidth());
				String text2 = null;
				int textWidth2 = 0;
				if (symbol.isPresent()) {
					text2 = String.format(" <%s>", symbol.get());
					g.setFont(smallFont);
					Rectangle2D bounds2 = g.getFontMetrics().getStringBounds(text2, g);
					textWidth2 = (int) Math.ceil(bounds2.getWidth());
				}
				if (textWidth1 + textWidth2 + stackRPad > stackRight) {
					stackRight = textWidth1 + textWidth2 + stackRPad;
					update();
				}
				g.setFont(normalFont);
				g.drawString(text1,
						insets.left + stackLeft + stackWidth + stackRPad,
						top - (int) Math.ceil(bounds1.getY()));
				if (text2 != null) {
					g.setFont(smallFont);
					g.drawString(text2,
							insets.left + stackLeft + stackWidth + stackRPad + textWidth1,
							top - (int) Math.ceil(bounds1.getY()));
				}
			});
		}
		
		private void drawAddr(Graphics2D g, int addr, Optional<String> optText) {
			g.setFont(smallFont);
			
			boolean textAbove = optText.isPresent();
			String text = optText.orElseGet(() -> String.format("0x%08x", addr));
			
			Insets insets = getInsets();
			int y = addrToY(addr);
			Rectangle2D bounds = g.getFontMetrics().getStringBounds(text, g);
			int textWidth = (int) Math.ceil(bounds.getWidth());
			if (textWidth + stackLPad > stackLeft) {
				stackLeft = textWidth + stackLPad;
				update();
			}
			int left = insets.left + stackLeft - stackLPad - textWidth;
			g.setColor(Color.BLACK);
			g.drawString(text, left, y + (int) Math.ceil(textAbove
					? -(bounds.getHeight() + bounds.getY())
					: -bounds.getY()));
			
			g.drawLine(left, y, insets.left + stackLeft, y);
			Polygon arrowHead = new Polygon();
			arrowHead.addPoint(insets.left + stackLeft, y);
			arrowHead.addPoint(insets.left + stackLeft - 6, y - 3);
			arrowHead.addPoint(insets.left + stackLeft - 6, y + 3);
			g.fillPolygon(arrowHead);
		}
		
		private void drawFrameSize(Graphics2D g, Frame frame) {
			AffineTransform transform = g.getTransform();
			g.setFont(smallFont);
			
			Insets insets = getInsets();
			int mid = addrToY(frame.getBase()) - (frame.getBase() - frame.getTop()) * scale / 2;
			String text = String.format("s:%d", frame.getBase() - frame.getTop());
			Rectangle2D bounds = g.getFontMetrics().getStringBounds(text, g);
			int textHeight = (int) Math.ceil(bounds.getHeight());
			if (textHeight > stackLPad) {
				stackLPad = textHeight;
				update();
			}
			int right = insets.left + stackLeft;
			int textHalfWidth = (int) Math.ceil(bounds.getWidth() / 2);
			int textDescend = (int) Math.ceil(bounds.getY() + bounds.getHeight());
			g.rotate(Math.toRadians(-90), right, mid);
			g.setColor(Color.WHITE);
			int outlineRadius = 2;
			int outlineMax = 3;
			for (int i = -outlineRadius; i <= outlineRadius; i++) {
				for (int j = -outlineRadius; j <= outlineRadius; j++) {
					if (Math.abs(i) + Math.abs(j) <= outlineMax) {
						g.drawString(text, right - textHalfWidth + i, mid - textDescend + j);
					}
				}
			}
			g.setColor(Color.BLACK);
			g.drawString(text, right - textHalfWidth, mid - textDescend);
			
			g.setTransform(transform);
		}
		
		private void drawFrameName(Graphics2D g, Frame frame, String name) {
			AffineTransform transform = g.getTransform();
			g.setFont(smallFont);
			
			Insets insets = getInsets();
			int mid = addrToY(frame.getBase()) - (frame.getBase() - frame.getTop()) * scale / 2;
			Rectangle2D bounds = g.getFontMetrics().getStringBounds(name, g);
			int textHeight = (int) Math.ceil(bounds.getHeight());
			if (textHeight > stackRPad) {
				stackRPad = textHeight;
				update();
			}
			int left = insets.left + stackLeft + stackWidth;
			int textHalfWidth = (int) Math.ceil(bounds.getWidth() / 2);
			int textAscend = (int) Math.ceil(-bounds.getY());
			g.rotate(Math.toRadians(-90), left, mid);
			g.setColor(Color.BLACK);
			g.drawString(name, left - textHalfWidth, mid + textAscend);
			
			g.setTransform(transform);
		}
	}
	
	private static class HelpDialog extends JDialog {
		public HelpDialog() {
			super();
			String helpContent = """
					<h1>Callstack Analyzer</h1>
					<p>The <i>Callstack Analyzer</i> is an additional tool for RARS which allows for
					tracking the state of the call stack while executing a program.</p>
					
					<h2>User Interface</h2>
					<p>On the left, there are the settings. Below that, special events are being
					logged. Below that, the stack pointer, frame pointer, and program counter
					registers are being displayed.</p>
					<p>On the right, there is a call stack diagram. Depending on the selection in
					the settings, there may be multiple call stack diagrams to be compared by the
					user. It is also possible to choose whether the "s0" register should be called
					by its alternative name "fp" to clarify its function as the frame pointer. "fp"
					will be treated as the default name from now on.</p>
					<p>At the bottom, there is a control bar which is supplied by RARS itself. To
					use the <i>Callstack Analyzer</i>, it is necessary to press <i>Connect</i>
					before starting the program. Before starting the program again, <i>Reset</i>
					should be pressed.</p>
					
					<h2>Functionality</h2>
					<p>The structure of the call stack is not directly included in the program. It
					is only possible to employ different strategies in odrder to try reconstructing
					the call stack as well as possible. The <i>Callstack Analyzer</i> therefore
					provides multiple different strategies which each require different knowledge
					about the running program.</p>
					
					<h3>Strategy: No Frames</h3>
					<p>required knowledge:</p>
					<ul>
						<li>current value of the "sp" register</li>
					</ul>
					<p>behavior:</p>
					<ul>
						<li>The whole call stack is being displayed.</li>
					</ul>
					<p>advantage:</p>
					<ul>
						<li>The program could do this analysis on its own in order to measure its
						own stack memory usage.</li>
						<li>The program does not have to use "s0" as frame pointer.</li>
					</ul>
					<p>limitations:</p>
					<ul>
						<li>This strategy cannot differentiate between multiple stack frames.</li>
					</ul>
					
					<h3>Strategy: Only Stack Pointer</h3>
					<p>required knowledge:</p>
					<ul>
						<li>every change of the "sp" register</li>
					</ul>
					<p>behavior:</p>
					<ul>
						<li>Each time the "sp" register changes, a decrease is being treated as a
						new stack frame and an increase as removing a stack frame.</li>
					</ul>
					<p>advantages:</p>
					<ul>
						<li>The program does not have to use "s0" as frame pointer.</li>
						<li>Nevertheless, all stack frames are mostly recognizable</li>
					</ul>
					<p>limitations:</p>
					<ul>
						<li>If a function chooses to increase the size of its frame, this will be
						treated as an entirely new frame. (This behavior is not usual though.)</li>
						<li>The program itself could not do this analysis on its own.</li>
					</ul>
					
					<h3>Strategy: Call And Return</h3>
					<p>required knowledge:</p>
					<ul>
						<li>current value of the "sp" register and the "fp" register</li>
						<li>value of the "sp" register and the "fp" register each time "call" or
						"ret" is being executed.</li>
					</ul>
					<p>behavior:</p>
					<ul>
						<li>top stack frame is always being updated with "sp" and "fp"</li>
						<li>"call" creates new stack frame, "ret" removes the top stack frame</li>
					</ul>
					<p>advantages:</p>
					<ul>
						<li>Each function call corresponds with exactly one stack frame.</li>
					</ul>
					<p>limitations:</p>
					<ul>
						<li>The program itself could not do this analysis on its own.</li>
					</ul>
					
					<h3>Strategy: Stack Unwinding</h3>
					<p>required knowledge:</p>
					<ul>
						<li>current value of the "sp" register and the "fp" register</li>
						<li>position of the saved "fp" register in the stack frame</li>
						<li>content of the stack memory</li>
					</ul>
					<p>behavior:</p>
					<ul>
						<li>The algorithm starts at the current stack frame as specified by "fp" and
						retrieves the previous value of "fp", the pointer to the previous stack
						frame.</li>
						<li>Using this principle, for each frame, the previous frame is being
						discovered until the algorithm has reached the upper end of the stack
						memory, the first stack frame.</li>
						<li>Basically, the stack is being treated as a linked list of frames.</li>
					</ul>
					<p>advantages:</p>
					<ul>
						<li>The program could do this analysis on its own. (Indeed, similar
						approaches are being taken to create stack traces etc.)</li>
					</ul>
					<p>limitations:</p>
					<ul>
						<li>The program must use "s0" as frame pointer.</li>
						<li>"fp" must be saved as the second word in the stack frame, addressable
						via <code>+8(fp)</code>.</li>
					</ul>
					
					<h2>Example</h2>
					<p>The following example illustrates a simple use case. The program start at
					<code>_start</code>. The function <code>printnumbers</code> is being called
					which in turn calls the function <code>printdigit</code> multiple times.</p>
					<pre>
					.eqv SYS_PrintInt, 1
					.eqv SYS_Exit, 10
					.eqv SYS_PrintChar, 11
					
					_start:
						mv fp, sp
						
						li a0, 4
						call printnumbers
						
						li a7, SYS_Exit
						ecall
					
					# void printnumbers(int n)
					printnumbers:
						addi sp, sp, -16
						sw ra, 12(sp)
						sw fp, 8(sp)
						sw s1, 4(sp)
						sw s2, 0(sp)
						addi fp, sp, 16
						
						mv s1, a0
						li s2, 0
						
					loop:
						mv a0, s2
						call printdigit
						addi s2, s2, 1
						blt s2, s1, loop
						
						lw ra, 12(sp)
						lw fp, 8(sp)
						lw s1, 4(sp)
						lw s2, 0(sp)
						addi sp, sp, 16
						ret
					
					# void printdigit(int n)
					printdigit:
						addi sp, sp, -16
						sw ra, 12(sp)
						sw fp, 8(sp)
						addi fp, sp, 16
						
						addi a0, a0, '0'
						li a7, SYS_PrintChar
						ecall
						
						lw ra, 12(sp)
						lw fp, 8(sp)
						addi sp, sp, 16
						ret
					</pre>
					<p>All strategies can be used to reconstruct the call stack for this program.
					</p>
					""";
			JEditorPane helpEditorPane = new JEditorPane("text/html", helpContent);
			helpEditorPane.setEditable(false);
			helpEditorPane.setCaretPosition(0);
			JScrollPane helpScrollPane = new JScrollPane(helpEditorPane);
			helpScrollPane.setPreferredSize(new Dimension(500, 400));
			add(helpScrollPane);
			pack();
		}
	}
}
