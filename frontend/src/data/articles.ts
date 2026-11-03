export const ARTICLES = [
  {
    slug: 'chon-cpu-va-gpu-cho-pc-gaming',
    title: 'Chọn CPU và GPU: cân bằng trước, nâng cấp sau',
    excerpt:
      'Bắt đầu từ tựa game, màn hình và ngân sách để chọn bộ đôi xử lý phù hợp.',
    category: 'Cấu hình',
    imageCategory: 'pc-gaming',
    readMinutes: 5,
    date: '2026-10-25',
    intro:
      'Một dàn PC hợp lý không nhất thiết có linh kiện đắt nhất. Điều đáng quan tâm là các bộ phận có cùng phục vụ tốt nhu cầu của bạn hay không. Trước khi chọn model, hãy viết ra ba điều: bạn dùng máy để làm gì, đang có màn hình nào và tổng ngân sách bao nhiêu.',
    takeaway: 'Chọn trải nghiệm bạn muốn có, rồi mới chọn tên linh kiện.',
    sections: [
      {
        heading: 'Bắt đầu từ trò chơi và màn hình',
        paragraphs: [
          'Một người chơi game thi đấu ở mức thiết lập gọn nhẹ có ưu tiên khác với người muốn ngắm đồ họa trong các trò chơi thế giới mở. Hãy chọn vài tựa game bạn thực sự chơi, ghi lại độ phân giải và mức khung hình mong muốn. Đó là căn cứ hữu ích hơn một bảng xếp hạng linh kiện chung.',
          'Nếu đã có màn hình, hãy xem độ phân giải, tần số quét và cổng kết nối của nó. Nếu chưa có, nên dự trù màn hình ngay trong ngân sách. Đầu tư toàn bộ vào thùng máy rồi phải giảm mạnh chất lượng phần hiển thị sẽ khiến trải nghiệm khó cân đối.',
        ],
      },
      {
        heading: 'Hiểu công việc của CPU và GPU',
        paragraphs: [
          'CPU xử lý nhiều công việc của trò chơi và hệ thống, còn GPU đảm nhiệm phần lớn quá trình dựng hình. Khi một bộ phận không theo kịp phần còn lại, hiệu năng tổng thể có thể bị giới hạn. Intel cũng nhấn mạnh rằng mức cân bằng còn phụ thuộc từng trò chơi và thiết lập sử dụng.',
          'Vì vậy, đừng xem một con số “nghẽn cổ chai” trên mạng như kết luận cho mọi tình huống. Hãy tìm bài đo đúng cặp CPU, GPU và độ phân giải dự định dùng. So sánh các tình huống gần với cách bạn sử dụng máy sẽ có giá trị hơn so sánh tên phân khúc.',
        ],
      },
      {
        heading: 'Chừa chỗ cho các phần ít được chú ý',
        paragraphs: [
          'RAM, SSD, nguồn và tản nhiệt vẫn là những khoản cần tính đủ. Một cấu hình chỉ đẹp ở cặp CPU/GPU nhưng thiếu không gian lưu trữ hay không phù hợp với vỏ case sẽ tạo ra nhiều việc phải giải quyết sau khi mua.',
          'Hãy chia bảng dự toán thành thùng máy, màn hình và thiết bị điều khiển. Sau đó kiểm tra lại từng bộ phận trong thùng máy. Nếu vượt ngân sách, điều chỉnh mục tiêu trải nghiệm hoặc một vài lựa chọn thay vì tự động cắt các khoản hỗ trợ hệ thống.',
        ],
        bullets: [
          'Kiểm tra socket CPU và nền tảng mainboard.',
          'Đối chiếu loại RAM, kích thước GPU và giới hạn của case.',
          'Xem yêu cầu nguồn điện từ hãng GPU.',
          'Dự trù cả màn hình, bàn phím và chuột nếu chưa có.',
        ],
      },
    ],
    relatedCategories: ['pc-gaming', 'cpu', 'vga'],
    sources: [
      {
        name: 'Intel — What Is Bottlenecking My PC?',
        url: 'https://www.intel.com/content/www/us/en/gaming/resources/what-is-bottlenecking-my-pc.html',
      },
      {
        name: 'Intel — How to Build a Gaming PC',
        url: 'https://www.intel.com/content/www/us/en/gaming/resources/how-to-build-a-gaming-pc.html',
      },
    ],
  },
  {
    slug: 'tan-nhiet-khi-hay-tan-nhiet-nuoc-aio',
    title: 'Tản nhiệt khí hay AIO: chọn theo bộ máy của bạn',
    excerpt:
      'Một bộ tản phù hợp bắt đầu từ CPU, không gian case và cách bạn sử dụng máy.',
    category: 'Tản nhiệt',
    imageCategory: 'cooling',
    readMinutes: 5,
    date: '2026-10-25',
    intro:
      'Đứng giữa một bộ tản khí lớn và một AIO có radiator, bạn có thể dễ bị thu hút bởi hình thức trước. Cách chọn thực tế hơn là bắt đầu từ CPU cần làm mát, thời gian làm việc ở tải cao và không gian lắp đặt trong case.',
    takeaway:
      'Khả năng lắp vừa và phù hợp với tải sử dụng quan trọng hơn số đèn RGB.',
    sections: [
      {
        heading: 'Tản khí: cấu tạo đơn giản, dễ chăm sóc',
        paragraphs: [
          'Tản khí dùng khối kim loại, ống dẫn nhiệt và quạt để đưa nhiệt khỏi CPU. Cấu trúc không có bơm là một điểm thuận lợi khi bạn ưu tiên sự đơn giản và khả năng bảo trì. Noctua lưu ý rằng quạt của tản khí có thể thay thế khi cần.',
          'Trong một cấu hình sử dụng hằng ngày, hãy xem chiều cao tản mà case cho phép và khoảng hở với RAM. Đừng chỉ nhìn kích thước quạt: một khối tản vừa socket nhưng vướng thanh RAM cao vẫn có thể làm kế hoạch lắp ráp trở nên khó khăn.',
        ],
      },
      {
        heading: 'AIO: cần lên kế hoạch chỗ đặt radiator',
        paragraphs: [
          'AIO có thêm bơm, ống dẫn và radiator. Các mẫu radiator lớn có thể đáp ứng tải nhiệt cao, nhưng hiệu quả thực tế còn liên quan mẫu tản, luồng khí và cách bố trí case. Theo Noctua, vị trí radiator cũng có thể ảnh hưởng đến luồng nhiệt trong hệ thống.',
          'Hãy kiểm tra đúng kích thước radiator được case hỗ trợ ở nóc hoặc mặt trước. Chừa không gian cho quạt, mainboard và đường ống. Con số “hỗ trợ 360 mm” trên case chưa đủ để kết luận mọi bộ tản 360 mm đều lắp thuận lợi cùng các linh kiện khác.',
        ],
      },
      {
        heading: 'Chọn theo ưu tiên sử dụng',
        paragraphs: [
          'Nếu muốn một bộ máy dễ chăm sóc, bắt đầu bằng một tản khí phù hợp là hướng đáng cân nhắc. Nếu dành nhiều thời gian cho công việc nặng hoặc muốn bố cục trong case thoáng quanh socket, bạn có thể đưa AIO vào danh sách so sánh.',
          'Dù chọn loại nào, hãy hoàn tất bảng kiểm tương thích trước khi mua. Lắp đặt đúng hướng dẫn và bố trí luồng khí hợp lý giúp bạn tận dụng thiết bị đã chọn. Nếu chưa tự tin, nên nhờ người có kinh nghiệm đối chiếu case, mainboard và bộ gá.',
        ],
        bullets: [
          'Đúng socket và có bộ gá đi kèm.',
          'Lắp vừa case, không vướng RAM hoặc GPU.',
          'Đủ cổng cắm và bộ điều khiển quạt cần thiết.',
          'Có hướng dẫn lắp đặt rõ ràng cho đúng model.',
        ],
      },
    ],
    relatedCategories: ['cooling', 'case', 'psu'],
    sources: [
      {
        name: 'Noctua — Air cooling or water cooling?',
        url: 'https://www.noctua.at/cn/expertise/guides/air-cooling-or-water-cooling-which-one-to-choose',
      },
      {
        name: 'Noctua — CPU cooler buying guide',
        url: 'https://www.noctua.at/en/cpu-cooler-buying-guide',
      },
    ],
  },
  {
    slug: 'chon-ram-va-ssd-khi-nang-cap-pc',
    title: 'Nâng cấp RAM và SSD: kiểm tra đúng trước khi mua',
    excerpt:
      'Phân biệt dung lượng, chuẩn kết nối và khả năng tương thích để nâng cấp gọn gàng.',
    category: 'Nâng cấp',
    imageCategory: 'storage',
    readMinutes: 5,
    date: '2026-10-25',
    intro:
      'Máy chậm khi mở nhiều ứng dụng và máy thiếu chỗ lưu game là hai vấn đề khác nhau. RAM và SSD có thể cùng nằm trong danh sách nâng cấp, nhưng bạn sẽ chọn tốt hơn nếu xác định việc nào đang khiến mình khó chịu nhất.',
    takeaway:
      'Ghi lại model mainboard và linh kiện đang dùng trước khi chọn bộ nhớ mới.',
    sections: [
      {
        heading: 'RAM: đúng thế hệ, đúng cách bố trí',
        paragraphs: [
          'Trước tiên, xem máy đang dùng DDR4 hay DDR5 và mainboard hỗ trợ loại nào. Không chọn RAM chỉ dựa trên dung lượng hoặc tốc độ quảng cáo. Số khe cắm, CPU và hướng dẫn của mainboard đều cần được đối chiếu.',
          'Kingston giải thích rằng các hệ thống dual-channel thường sử dụng cặp module giống nhau để tối ưu băng thông. Khi muốn tăng dung lượng, hãy xem hướng dẫn bố trí khe của đúng mainboard. Trộn nhiều bộ kit không tự động bảo đảm giữ được tốc độ hoặc mức ổn định mong muốn.',
        ],
      },
      {
        heading: 'SSD: M.2 là kiểu dáng, NVMe là một phần khác',
        paragraphs: [
          'Thấy một thanh SSD M.2 không có nghĩa là đã xác định được toàn bộ chuẩn kết nối của nó. Kingston nêu rõ M.2 có thể đi cùng giao tiếp SATA hoặc PCIe. Bởi vậy, cần kiểm tra cả chuẩn được khe cắm hỗ trợ và chiều dài module.',
          'Với SSD NVMe, hãy đối chiếu thế hệ PCIe và cách mainboard phân chia tài nguyên cho từng khe. Một số khe M.2 chia sẻ đường kết nối với thiết bị khác. Sách hướng dẫn mainboard là nơi nên xem trước khi quyết định lắp thêm ổ.',
        ],
      },
      {
        heading: 'Chọn dung lượng theo dữ liệu thực tế',
        paragraphs: [
          'Hãy mở danh sách game, phần mềm và thư mục công việc để ước lượng dung lượng cần có. Nếu chỉ nhìn tốc độ đọc tuần tự, bạn có thể bỏ qua việc mình đang cần thêm không gian lưu trữ hơn là một mức thông số cao hơn.',
          'Trước khi chuyển dữ liệu, hãy sao lưu những tệp quan trọng sang một nơi riêng. Sau khi lắp, kiểm tra máy đã nhận đúng dung lượng và thiết bị chưa. Giữ lại thông tin model, ngày mua và hướng dẫn của hãng để tiện quản lý về sau.',
        ],
        bullets: [
          'Ghi lại model mainboard và CPU.',
          'Kiểm tra DDR4/DDR5, số khe RAM còn trống.',
          'Đối chiếu SATA/NVMe, kích thước và khe M.2.',
          'Sao lưu dữ liệu trước khi chuyển hoặc cài lại hệ thống.',
        ],
      },
    ],
    relatedCategories: ['ram', 'storage', 'mainboard'],
    sources: [
      {
        name: 'Kingston — Memory population rules',
        url: 'https://www.kingston.com/unitedkingdom/en/memory/memory-population-rules',
      },
      {
        name: 'Kingston — FAQs for SATA and M.2 SSDs',
        url: 'https://www.kingston.com/en/ssd/ssd-faq',
      },
      {
        name: 'Kingston — Types of SSD form factors',
        url: 'https://www.kingston.com/en/blog/pc-performance/ssd-form-factors',
      },
    ],
  },
  {
    slug: 'chon-chuot-va-ban-phim-cho-goc-may',
    title: 'Chọn chuột và bàn phím cho một góc máy dễ dùng',
    excerpt:
      'Cảm giác cầm, layout phím và kết nối đáng được thử trước khi bạn quyết định.',
    category: 'Góc máy',
    imageCategory: 'keyboard',
    readMinutes: 4,
    date: '2026-10-25',
    intro:
      'Chuột và bàn phím là những thứ bạn chạm vào mỗi ngày. Một mẫu được nhiều người yêu thích chưa chắc đã hợp bàn tay, thói quen gõ và không gian của bạn. Hãy bắt đầu bằng các thao tác mình thường làm, rồi thử cảm giác thực tế nếu có thể.',
    takeaway:
      'Một góc máy dễ dùng bắt đầu từ cảm giác thoải mái của chính bạn.',
    sections: [
      {
        heading: 'Chuột: thử dáng cầm trước thông số',
        paragraphs: [
          'Khi cầm thử, chú ý chỗ đặt ngón cái, vị trí nút phụ và khả năng nhấc chuột. Với cùng một dáng cầm, kích thước tay khác nhau vẫn có thể tạo ra cảm nhận khác nhau. Bạn nên thử cả thao tác chậm lẫn di chuyển rộng trên lót chuột.',
          'Trong so sánh dòng G502, Logitech cũng xem hình dáng, trọng lượng và cách bố trí nút là những yếu tố lựa chọn. Thay vì chỉ theo đuổi mức DPI lớn nhất, hãy xem bạn có dễ điều khiển chuột ở mức nhạy quen dùng hay không.',
        ],
      },
      {
        heading: 'Bàn phím: chọn layout và cảm giác bấm',
        paragraphs: [
          'Nếu thường nhập số, phần numpad có thể hữu ích. Nếu muốn dành nhiều diện tích cho chuột, bạn có thể cân nhắc layout gọn hơn. Hãy lập danh sách những phím thật sự dùng mỗi ngày để tránh phải đổi thói quen quá nhiều sau khi mua.',
          'Keychron phân biệt switch linear có hành trình trơn, tactile có khấc phản hồi và clicky có thêm tiếng click. Đây là điểm khởi đầu để thử phím, không phải quy tắc rằng một loại luôn phù hợp với mọi người. Môi trường làm việc chung cũng là điều nên cân nhắc khi chọn âm thanh.',
        ],
      },
      {
        heading: 'Kết nối và khả năng thay đổi về sau',
        paragraphs: [
          'Xác định xem bạn cần dùng với một máy hay chuyển giữa nhiều thiết bị. Với kết nối không dây, kiểm tra phụ kiện đi kèm và cách sạc hoặc thay pin. Với bàn phím có dây, hãy xem chiều dài và hướng đi dây trên bàn.',
          'Nếu dự định đổi switch, đọc thông số hot-swap của đúng model. Keychron lưu ý rằng loại switch và socket phải tương thích; không nên xem mọi switch cơ hoặc low-profile là dùng chung. Một bảng kiểm nhỏ sẽ giúp việc mua phụ kiện về sau dễ dàng hơn.',
        ],
        bullets: [
          'Thử dáng cầm và vị trí nút phụ của chuột.',
          'Chọn layout theo các phím thường dùng.',
          'Thử cảm giác và âm thanh switch.',
          'Đối chiếu kết nối, phụ kiện và khả năng thay switch.',
        ],
      },
    ],
    relatedCategories: ['keyboard', 'mouse'],
    sources: [
      {
        name: 'Logitech G — G502 HERO vs G502 X',
        url: 'https://www.logitechg.com/en-gb/discover/learn/g502-hero-vs-g502-x',
      },
      {
        name: 'Keychron — Mechanical keyboard switches',
        url: 'https://www.keychron.com/collections/all-switches',
      },
      {
        name: 'Keychron — Keyboard switch guide',
        url: 'https://www.keychron.com/collections/keyboard-switch',
      },
    ],
  },
]
